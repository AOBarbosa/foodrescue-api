package br.com.seudominio.foodrescue.business.forecast;

import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Predicate;

/**
 * Initial {@link DemandForecastStrategy}: averages, over past days, the units
 * sold in the same time slot that is left today (from now until closing).
 *
 * <p>Only past days on the same weekday as today are considered, since
 * demand usually follows a weekly pattern. Days without any sale in the slot
 * count as zero. When the history has no day on today's weekday yet, every
 * past day is used instead, with {@link ForecastConfidence#LOW} confidence.
 * The result is capped by the current stock, since no more units than the
 * available ones can be sold.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class WeekdayHourlyAverageForecastStrategy implements DemandForecastStrategy {

    private static final int MEDIUM_CONFIDENCE_MIN_DAYS = 2;
    private static final int HIGH_CONFIDENCE_MIN_DAYS = 4;

    @Override
    public ForecastResult forecast(ForecastContext context) {
        LocalDate today = context.now().toLocalDate();
        List<Sale> pastSales = context.salesHistory().stream()
                .filter(sale -> sale.getSoldAt().toLocalDate().isBefore(today))
                .toList();

        if (pastSales.isEmpty() || !context.now().isBefore(context.closingAt())) {
            return new ForecastResult(0, pastSales.isEmpty() ? ForecastConfidence.LOW : ForecastConfidence.HIGH, 0);
        }

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
        LocalTime slotEnd = context.closingAt().toLocalTime();
        List<Sale> slotSales = pastSales.stream()
                .filter(sale -> comparableDay.test(sale.getSoldAt().toLocalDate()))
                .filter(sale -> isWithin(sale.getSoldAt().toLocalTime(), slotStart, slotEnd))
                .toList();

        int unitsSold = slotSales.stream().mapToInt(Sale::getQuantity).sum();
        int average = (int) Math.round((double) unitsSold / observedDays);
        int predicted = Math.min(average, Math.max(context.currentStock(), 0));

        return new ForecastResult(predicted, confidence, slotSales.size());
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
