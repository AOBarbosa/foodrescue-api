package br.com.seudominio.perishrescue.business.forecast;

import br.com.seudominio.perishrescue.domain.entities.Sale;
import br.com.seudominio.perishrescue.domain.enums.ForecastConfidence;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Predicate;

/**
 * The framework's statistical {@link DemandForecastStrategy}, based only on
 * the resource's sales history. It adapts to the instance's
 * {@link ForecastWindow}:
 *
 * <ul>
 *   <li><b>Window within today</b> (e.g. FoodRescue, until closing): averages,
 *       over past days on the same weekday, the units sold in the same time
 *       slot that is left today. Days without any sale in the slot count as
 *       zero. When the history has no day on today's weekday yet, every past
 *       day is used instead, with {@link ForecastConfidence#LOW} confidence.</li>
 *   <li><b>Window of several days</b> (e.g. PharmaRescue, until the batch
 *       expires): the average units sold per day, over the history, times the
 *       days left in the window.</li>
 * </ul>
 *
 * <p>The result is capped by the current stock, since no more units than the
 * available ones can be sold. It is also the fallback of the AI strategy.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class HistoricalAverageForecastStrategy implements DemandForecastStrategy {

    /**
     * Source recorded for forecasts of a window within today.
     */
    public static final String SOURCE = "weekday-hourly-average";

    /**
     * Source recorded for forecasts of a window of several days.
     */
    public static final String DAILY_SOURCE = "daily-average";

    private static final int MEDIUM_CONFIDENCE_MIN_DAYS = 2;
    private static final int HIGH_CONFIDENCE_MIN_DAYS = 4;
    private static final int MEDIUM_CONFIDENCE_MIN_DAYS_OF_HISTORY = 7;
    private static final int HIGH_CONFIDENCE_MIN_DAYS_OF_HISTORY = 28;
    private static final double HOURS_PER_DAY = 24.0;

    @Override
    public ForecastResult forecast(ForecastContext context) {
        LocalDate today = context.now().toLocalDate();
        List<Sale> pastSales = context.salesHistory().stream()
                .filter(sale -> sale.getSoldAt().toLocalDate().isBefore(today))
                .toList();

        if (pastSales.isEmpty() || context.window().isOver()) {
            return new ForecastResult(0, pastSales.isEmpty() ? ForecastConfidence.LOW : ForecastConfidence.HIGH, 0,
                    sourceFor(context.window()), null);
        }

        return context.window().isSameDay()
                ? forecastRestOfToday(context, pastSales, today)
                : forecastSeveralDays(context, pastSales, today);
    }

    private ForecastResult forecastRestOfToday(ForecastContext context, List<Sale> pastSales, LocalDate today) {
        LocalDate firstDay = pastSales.get(0).getSoldAt().toLocalDate();
        DayOfWeek weekday = today.getDayOfWeek();
        long sameWeekdayDays = firstDay.datesUntil(today)
                .filter(day -> day.getDayOfWeek() == weekday)
                .count();

        Predicate<LocalDate> comparableDay;
        long observedDays;
        ForecastConfidence confidence;
        if (sameWeekdayDays > 0) {
            comparableDay = day -> day.getDayOfWeek() == weekday;
            observedDays = sameWeekdayDays;
            confidence = confidenceFor(sameWeekdayDays);
        } else {
            comparableDay = day -> true;
            observedDays = firstDay.datesUntil(today).count();
            confidence = ForecastConfidence.LOW;
        }

        LocalTime slotStart = context.now().toLocalTime();
        LocalTime slotEnd = context.window().until().toLocalTime();
        List<Sale> slotSales = pastSales.stream()
                .filter(sale -> comparableDay.test(sale.getSoldAt().toLocalDate()))
                .filter(sale -> isWithin(sale.getSoldAt().toLocalTime(), slotStart, slotEnd))
                .toList();

        int unitsSold = slotSales.stream().mapToInt(Sale::getQuantity).sum();
        int average = (int) Math.round((double) unitsSold / observedDays);
        return new ForecastResult(capped(average, context), confidence, slotSales.size(), SOURCE, null);
    }

    private ForecastResult forecastSeveralDays(ForecastContext context, List<Sale> pastSales, LocalDate today) {
        LocalDate firstDay = pastSales.get(0).getSoldAt().toLocalDate();
        long observedDays = Math.max(firstDay.datesUntil(today).count(), 1);
        int unitsSold = pastSales.stream().mapToInt(Sale::getQuantity).sum();
        double perDay = (double) unitsSold / observedDays;
        double daysLeft = context.window().remaining().toMinutes() / 60.0 / HOURS_PER_DAY;

        int predicted = (int) Math.round(perDay * daysLeft);
        ForecastConfidence confidence = observedDays >= HIGH_CONFIDENCE_MIN_DAYS_OF_HISTORY
                ? ForecastConfidence.HIGH
                : observedDays >= MEDIUM_CONFIDENCE_MIN_DAYS_OF_HISTORY ? ForecastConfidence.MEDIUM : ForecastConfidence.LOW;
        return new ForecastResult(capped(predicted, context), confidence, pastSales.size(), DAILY_SOURCE, null);
    }

    private static int capped(int predicted, ForecastContext context) {
        return Math.min(predicted, Math.max(context.currentStock(), 0));
    }

    private static String sourceFor(ForecastWindow window) {
        return window.isSameDay() ? SOURCE : DAILY_SOURCE;
    }

    private ForecastConfidence confidenceFor(long observedDays) {
        if (observedDays >= HIGH_CONFIDENCE_MIN_DAYS) {
            return ForecastConfidence.HIGH;
        }
        if (observedDays >= MEDIUM_CONFIDENCE_MIN_DAYS) {
            return ForecastConfidence.MEDIUM;
        }
        return ForecastConfidence.LOW;
    }

    private boolean isWithin(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && time.isBefore(end);
    }
}
