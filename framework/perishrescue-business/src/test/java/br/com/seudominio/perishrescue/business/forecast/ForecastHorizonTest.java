package br.com.seudominio.perishrescue.business.forecast;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.entities.Sale;
import br.com.seudominio.perishrescue.domain.enums.ForecastConfidence;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hot spot 4: the forecast horizon and the window it produces, and the
 * statistical strategy adapting to windows of several days.
 */
class ForecastHorizonTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 15, 0);

    @Test
    void theDefaultHorizonForecastsUntilTheResourcesCriticalDeadline() {
        TestResource resource = TestResource.builder().build();
        resource.setExpirationDate(LocalDate.of(2026, 11, 20));

        ForecastWindow window = new DeadlineForecastHorizon(ChronoUnit.DAYS).windowFor(resource, NOW);

        assertThat(window.from()).isEqualTo(NOW);
        assertThat(window.until()).isEqualTo(resource.criticalDeadline(NOW));
        assertThat(window.granularity()).isEqualTo(ChronoUnit.DAYS);
        assertThat(window.remainingUnits()).isEqualTo(2);
        assertThat(window.isSameDay()).isFalse();
    }

    @Test
    void aWindowKnowsHowMuchTimeIsLeftInItsOwnUnit() {
        ForecastWindow untilClosing = new ForecastWindow(NOW, NOW.toLocalDate().atTime(18, 0), ChronoUnit.HOURS);

        assertThat(untilClosing.remainingUnits()).isEqualTo(3);
        assertThat(untilClosing.isWithin(3)).isTrue();
        assertThat(untilClosing.isWithin(2)).isFalse();
        assertThat(untilClosing.isSameDay()).isTrue();
        assertThat(untilClosing.isOver()).isFalse();
        assertThat(new ForecastWindow(NOW, NOW.minusMinutes(1), ChronoUnit.HOURS).isOver()).isTrue();
    }

    @Test
    void theHistoricalStrategyProjectsTheDailyAverageOverAWindowOfSeveralDays() {
        List<Sale> history = new ArrayList<>();
        for (int day = 28; day >= 1; day--) {
            history.add(Sale.builder().quantity(2).unitPrice(Money.of(9.9))
                    .soldAt(NOW.minusDays(day).with(LocalTime.NOON)).build());
        }
        ForecastContext tenDaysUntilTheBatchExpires = ForecastContext.builder()
                .salesHistory(history)
                .currentStock(100)
                .now(NOW)
                .until(NOW.plusDays(10))
                .granularity(ChronoUnit.DAYS)
                .build();

        ForecastResult result = new HistoricalAverageForecastStrategy().forecast(tenDaysUntilTheBatchExpires);

        assertThat(result.predictedQuantity()).isEqualTo(20);       // 2 units/day x 10 days
        assertThat(result.confidence()).isEqualTo(ForecastConfidence.HIGH);
        assertThat(result.source()).isEqualTo(HistoricalAverageForecastStrategy.DAILY_SOURCE);
    }
}
