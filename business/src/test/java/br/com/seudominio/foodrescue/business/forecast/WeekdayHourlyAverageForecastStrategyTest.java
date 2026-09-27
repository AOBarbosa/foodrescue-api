package br.com.seudominio.foodrescue.business.forecast;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class WeekdayHourlyAverageForecastStrategyTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);
    private static final LocalDateTime CLOSING_AT = LocalDateTime.of(2026, 9, 14, 22, 0);

    private final WeekdayHourlyAverageForecastStrategy strategy = new WeekdayHourlyAverageForecastStrategy();

    private Sale sale(LocalDateTime soldAt, int quantity) {
        return Sale.builder()
                .quantity(quantity)
                .unitPrice(Money.of(5.0))
                .soldAt(soldAt)
                .build();
    }

    private ForecastContext context(List<Sale> history, int stock, LocalDateTime now) {
        return ForecastContext.builder()
                .salesHistory(history)
                .currentStock(stock)
                .now(now)
                .closingAt(CLOSING_AT)
                .build();
    }

    private List<Sale> fourWeeksOfSameWeekdaySales() {
        List<Sale> history = new ArrayList<>();
        for (int week = 4; week >= 1; week--) {
            LocalDateTime day = NOW.minusWeeks(week);
            history.add(sale(day.withHour(10), 7));
            history.add(sale(day.withHour(16), 3));
            history.add(sale(day.withHour(18), 2));
        }
        history.add(sale(NOW.minusDays(1).withHour(17), 20));
        return history;
    }

    @Test
    void averagesSameWeekdaySalesWithinRemainingTimeSlot() {
        ForecastResult result = strategy.forecast(context(fourWeeksOfSameWeekdaySales(), 50, NOW));

        assertThat(result.predictedQuantity()).isEqualTo(5);
        assertThat(result.confidence()).isEqualTo(ForecastConfidence.HIGH);
        assertThat(result.sampleSize()).isEqualTo(8);
    }

    @Test
    void capsPredictionAtCurrentStock() {
        ForecastResult result = strategy.forecast(context(fourWeeksOfSameWeekdaySales(), 3, NOW));

        assertThat(result.predictedQuantity()).isEqualTo(3);
    }

    @Test
    void countsSameWeekdaysWithoutSalesAsZero() {
        List<Sale> history = List.of(
                sale(NOW.minusWeeks(4).withHour(10), 1),
                sale(NOW.minusWeeks(2).withHour(17), 8));

        ForecastResult result = strategy.forecast(context(history, 50, NOW));

        assertThat(result.predictedQuantity()).isEqualTo(2);
        assertThat(result.confidence()).isEqualTo(ForecastConfidence.HIGH);
        assertThat(result.sampleSize()).isEqualTo(1);
    }

    @Test
    void reportsMediumConfidenceWithFewSameWeekdays() {
        List<Sale> history = List.of(
                sale(NOW.minusWeeks(2).withHour(17), 4),
                sale(NOW.minusWeeks(1).withHour(17), 6));

        ForecastResult result = strategy.forecast(context(history, 50, NOW));

        assertThat(result.predictedQuantity()).isEqualTo(5);
        assertThat(result.confidence()).isEqualTo(ForecastConfidence.MEDIUM);
    }

    @Test
    void fallsBackToAllPastDaysWithLowConfidenceWhenNoSameWeekdayYet() {
        List<Sale> history = List.of(
                sale(NOW.minusDays(3).withHour(17), 3),
                sale(NOW.minusDays(2).withHour(17), 3),
                sale(NOW.minusDays(1).withHour(17), 3));

        ForecastResult result = strategy.forecast(context(history, 50, NOW));

        assertThat(result.predictedQuantity()).isEqualTo(3);
        assertThat(result.confidence()).isEqualTo(ForecastConfidence.LOW);
    }

    @Test
    void ignoresSalesFromToday() {
        List<Sale> history = new ArrayList<>(fourWeeksOfSameWeekdaySales());
        history.add(sale(NOW.withHour(9), 100));

        ForecastResult result = strategy.forecast(context(history, 50, NOW));

        assertThat(result.predictedQuantity()).isEqualTo(5);
    }

    @Test
    void predictsZeroAfterClosingTime() {
        ForecastResult result = strategy.forecast(context(fourWeeksOfSameWeekdaySales(), 50, CLOSING_AT.plusMinutes(30)));

        assertThat(result.predictedQuantity()).isZero();
        assertThat(result.sampleSize()).isZero();
    }
}
