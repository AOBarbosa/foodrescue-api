package br.com.seudominio.foodrescue.business.forecast;

import br.com.seudominio.foodrescue.business.ai.GeminiClient;
import br.com.seudominio.foodrescue.business.ai.GeminiException;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@link DemandForecastStrategy} backed by Google Gemini. The product's daily
 * sales summary is sent to the model, which answers with the predicted
 * quantity, a confidence level and a short rationale.
 *
 * <p>The statistical {@link WeekdayHourlyAverageForecastStrategy} runs first:
 * its estimate is handed to the model as a reference and is returned as is
 * whenever Gemini is not configured, fails, times out or answers with
 * something unusable, so a forecast is always produced. The model's answer is
 * clamped to {@code [0, currentStock]}.</p>
 *
 * <p>Active when {@code app.forecast.provider=gemini}.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
@Primary
@ConditionalOnProperty(name = "app.forecast.provider", havingValue = "gemini")
public class GeminiDemandForecastStrategy implements DemandForecastStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeminiDemandForecastStrategy.class);
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_RATIONALE_LENGTH = 1000;

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "predictedQuantity", Map.of("type", "INTEGER"),
                    "confidence", Map.of("type", "STRING", "enum", List.of("LOW", "MEDIUM", "HIGH")),
                    "rationale", Map.of("type", "STRING")),
            "required", List.of("predictedQuantity", "confidence", "rationale"));

    private final GeminiClient geminiClient;
    private final WeekdayHourlyAverageForecastStrategy fallback;

    /**
     * Constructor.
     *
     * @param geminiClient the Gemini API client
     * @param fallback     the statistical strategy used as reference and fallback
     */
    public GeminiDemandForecastStrategy(GeminiClient geminiClient, WeekdayHourlyAverageForecastStrategy fallback) {
        this.geminiClient = geminiClient;
        this.fallback = fallback;
    }

    @Override
    public ForecastResult forecast(ForecastContext context) {
        ForecastResult baseline = fallback.forecast(context);
        if (!geminiClient.isConfigured() || !context.now().isBefore(context.closingAt())) {
            return baseline;
        }

        try {
            JsonNode answer = geminiClient.generateJson(buildPrompt(context, baseline), RESPONSE_SCHEMA);
            return toResult(answer, context, baseline);
        } catch (GeminiException | IllegalArgumentException e) {
            LOGGER.warn("Gemini demand forecast failed, using {} instead: {}",
                    WeekdayHourlyAverageForecastStrategy.SOURCE, e.getMessage());
            return baseline;
        }
    }

    private ForecastResult toResult(JsonNode answer, ForecastContext context, ForecastResult baseline) {
        JsonNode quantity = answer.path("predictedQuantity");
        if (!quantity.canConvertToInt()) {
            throw new IllegalArgumentException("predictedQuantity missing from Gemini answer");
        }
        int predicted = Math.clamp(quantity.asInt(), 0, Math.max(context.currentStock(), 0));
        ForecastConfidence confidence = ForecastConfidence.valueOf(answer.path("confidence").asText());

        String rationale = answer.path("rationale").asText(null);
        if (rationale != null && rationale.length() > MAX_RATIONALE_LENGTH) {
            rationale = rationale.substring(0, MAX_RATIONALE_LENGTH);
        }
        return new ForecastResult(predicted, confidence, baseline.sampleSize(),
                "gemini:" + geminiClient.getModel(), rationale);
    }

    private String buildPrompt(ForecastContext context, ForecastResult baseline) {
        LocalDate today = context.now().toLocalDate();
        LocalTime slotStart = context.now().toLocalTime();
        LocalTime slotEnd = context.closingAt().toLocalTime();
        String slot = HOUR.format(slotStart) + "-" + HOUR.format(slotEnd);

        StringBuilder prompt = new StringBuilder()
                .append("Você é um analista de demanda de um pequeno estabelecimento de alimentos. ")
                .append("Estime quantas unidades do produto abaixo ainda serão vendidas hoje, das ")
                .append(HOUR.format(slotStart)).append(" até o fechamento às ").append(HOUR.format(slotEnd))
                .append(".\n\n")
                .append("Produto: ").append(context.productName())
                .append(" (categoria: ").append(context.productCategory()).append(")\n")
                .append("Estoque atual: ").append(context.currentStock()).append(" unidades\n")
                .append("Hoje: ").append(weekday(today)).append(", ").append(today).append("\n\n")
                .append("Histórico diário (data, dia da semana, unidades vendidas entre ").append(slot)
                .append(", unidades vendidas no dia inteiro):\n");

        Map<LocalDate, int[]> daily = dailySummary(context.salesHistory(), today, slotStart, slotEnd);
        if (daily.isEmpty()) {
            prompt.append("- sem vendas registradas\n");
        } else {
            LocalDate first = daily.keySet().iterator().next();
            first.datesUntil(today).forEach(day -> {
                int[] units = daily.getOrDefault(day, new int[2]);
                prompt.append("- ").append(day).append(" (").append(weekday(day)).append("): ")
                        .append(units[0]).append(" no intervalo, ").append(units[1]).append(" no dia\n");
            });
        }

        return prompt.append("\nEstimativa estatística de referência (média do mesmo intervalo em dias ")
                .append("comparáveis): ").append(baseline.predictedQuantity()).append(" unidades, confiança ")
                .append(baseline.confidence()).append(".\n\n")
                .append("Regras: predictedQuantity é um inteiro entre 0 e o estoque atual; confidence é LOW, ")
                .append("MEDIUM ou HIGH conforme a quantidade e a consistência do histórico; rationale é ")
                .append("uma frase curta em português explicando a estimativa.")
                .toString();
    }

    private Map<LocalDate, int[]> dailySummary(List<Sale> sales, LocalDate today,
                                               LocalTime slotStart, LocalTime slotEnd) {
        Map<LocalDate, int[]> daily = new TreeMap<>();
        for (Sale sale : sales) {
            LocalDate day = sale.getSoldAt().toLocalDate();
            if (!day.isBefore(today)) {
                continue;
            }
            int[] units = daily.computeIfAbsent(day, d -> new int[2]);
            LocalTime time = sale.getSoldAt().toLocalTime();
            if (!time.isBefore(slotStart) && time.isBefore(slotEnd)) {
                units[0] += sale.getQuantity();
            }
            units[1] += sale.getQuantity();
        }
        return daily;
    }

    private String weekday(LocalDate day) {
        return day.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR);
    }
}
