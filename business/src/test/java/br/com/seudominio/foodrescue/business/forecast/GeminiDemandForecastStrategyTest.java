package br.com.seudominio.foodrescue.business.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.ai.GeminiClient;
import br.com.seudominio.foodrescue.business.ai.GeminiException;
import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class GeminiDemandForecastStrategyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);
    private static final LocalDateTime CLOSING_AT = LocalDateTime.of(2026, 9, 14, 22, 0);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GeminiClient geminiClient;
    private GeminiDemandForecastStrategy strategy;

    @BeforeEach
    void setUp() {
        geminiClient = mock(GeminiClient.class);
        when(geminiClient.isConfigured()).thenReturn(true);
        when(geminiClient.getModel()).thenReturn("gemini-test");
        strategy = new GeminiDemandForecastStrategy(geminiClient, new WeekdayHourlyAverageForecastStrategy());
    }

    private ForecastContext context(int stock, LocalDateTime now) {
        List<Sale> history = new ArrayList<>();
        for (int week = 4; week >= 1; week--) {
            history.add(Sale.builder()
                    .quantity(5)
                    .unitPrice(Money.of(4.5))
                    .soldAt(NOW.minusWeeks(week).withHour(17))
                    .build());
        }
        return ForecastContext.builder()
                .productName("Pão de queijo")
                .productCategory("salgado")
                .salesHistory(history)
                .currentStock(stock)
                .now(now)
                .closingAt(CLOSING_AT)
                .build();
    }

    private void answer(String json) throws Exception {
        when(geminiClient.generateJson(anyString(), anyMap())).thenReturn(objectMapper.readTree(json));
    }

    @Test
    void usesGeminiAnswer() throws Exception {
        answer("{\"predictedQuantity\": 6, \"confidence\": \"MEDIUM\", \"rationale\": \"Vendas estáveis.\"}");

        ForecastResult result = strategy.forecast(context(30, NOW));

        assertThat(result.predictedQuantity()).isEqualTo(6);
        assertThat(result.confidence()).isEqualTo(ForecastConfidence.MEDIUM);
        assertThat(result.rationale()).isEqualTo("Vendas estáveis.");
        assertThat(result.source()).isEqualTo("gemini:gemini-test");
        assertThat(result.sampleSize()).isEqualTo(4);
    }

    @Test
    void sendsProductAndDailyHistoryInPrompt() throws Exception {
        answer("{\"predictedQuantity\": 5, \"confidence\": \"HIGH\", \"rationale\": \"ok\"}");

        strategy.forecast(context(30, NOW));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(geminiClient).generateJson(prompt.capture(), anyMap());
        assertThat(prompt.getValue())
                .contains("Pão de queijo")
                .contains("Estoque atual: 30")
                .contains("2026-09-07 (segunda-feira): 5 no intervalo, 5 no dia")
                .contains("2026-09-08 (terça-feira): 0 no intervalo, 0 no dia")
                .contains("referência")
                .contains("5 unidades, confiança HIGH");
    }

    @Test
    void clampsGeminiAnswerToCurrentStock() throws Exception {
        answer("{\"predictedQuantity\": 50, \"confidence\": \"HIGH\", \"rationale\": \"ok\"}");

        assertThat(strategy.forecast(context(3, NOW)).predictedQuantity()).isEqualTo(3);
    }

    @Test
    void fallsBackToStatisticalForecastWhenGeminiFails() {
        when(geminiClient.generateJson(anyString(), anyMap())).thenThrow(new GeminiException("HTTP 503"));

        ForecastResult result = strategy.forecast(context(30, NOW));

        assertThat(result.source()).isEqualTo(WeekdayHourlyAverageForecastStrategy.SOURCE);
        assertThat(result.predictedQuantity()).isEqualTo(5);
    }

    @Test
    void fallsBackWhenGeminiAnswerIsUnusable() throws Exception {
        answer("{\"predictedQuantity\": 5, \"confidence\": \"SURE\", \"rationale\": \"ok\"}");

        assertThat(strategy.forecast(context(30, NOW)).source())
                .isEqualTo(WeekdayHourlyAverageForecastStrategy.SOURCE);
    }

    @Test
    void skipsGeminiWhenNotConfigured() {
        when(geminiClient.isConfigured()).thenReturn(false);

        ForecastResult result = strategy.forecast(context(30, NOW));

        assertThat(result.source()).isEqualTo(WeekdayHourlyAverageForecastStrategy.SOURCE);
        verify(geminiClient, never()).generateJson(any(), any());
    }

    @Test
    void skipsGeminiAfterClosingTime() {
        ForecastResult result = strategy.forecast(context(30, CLOSING_AT.plusMinutes(10)));

        assertThat(result.predictedQuantity()).isZero();
        verify(geminiClient, never()).generateJson(any(), any());
    }
}
