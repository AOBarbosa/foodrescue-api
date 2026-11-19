package br.com.seudominio.perishrescue.business.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;

import org.junit.jupiter.api.Test;

class DiscountThenPriorityStrategyTest {

    private final DiscountThenPriorityStrategy strategy = new DiscountThenPriorityStrategy(10, 60);

    private SurplusContext context(double currentDiscount) {
        return new SurplusContext(Percentage.of(80), 20, 16,
                new br.com.seudominio.perishrescue.business.forecast.ForecastWindow(java.time.LocalDateTime.of(2026, 11, 18, 18, 0), java.time.LocalDateTime.of(2026, 11, 18, 18, 0).plusHours(2), java.time.temporal.ChronoUnit.HOURS), Percentage.of(currentDiscount),
                RecommendationType.DISCOUNT);
    }

    @Test
    void deepensTheDiscountByTheStep() {
        SurplusDecision decision = strategy.decide(context(40));

        assertThat(decision.action()).isEqualTo(SurplusAction.ADDITIONAL_DISCOUNT);
        assertThat(decision.percentage().value()).isEqualByComparingTo("50");
    }

    @Test
    void capsTheDiscountAtTheMaximum() {
        assertThat(strategy.decide(context(55)).percentage().value()).isEqualByComparingTo("60");
    }

    @Test
    void prioritizesWithoutHeadroom() {
        SurplusDecision decision = strategy.decide(context(60));

        assertThat(decision.action()).isEqualTo(SurplusAction.PRIORITIZE);
        assertThat(decision.percentage()).isNull();
    }

    private SurplusContext exhausted(RecommendationType previous) {
        return new SurplusContext(Percentage.of(80), 20, 16,
                new br.com.seudominio.perishrescue.business.forecast.ForecastWindow(
                        java.time.LocalDateTime.of(2026, 11, 18, 18, 0),
                        java.time.LocalDateTime.of(2026, 11, 18, 20, 0), java.time.temporal.ChronoUnit.HOURS),
                Percentage.of(60), previous);
    }

    @Test
    void neverSuggestsADonationUnlessTheInstanceEnablesIt() {
        assertThat(strategy.decide(exhausted(RecommendationType.SURPLUS_DESTINATION)).action())
                .isEqualTo(SurplusAction.PRIORITIZE);
    }

    @Test
    void suggestsADonationWhenEnabledAndThePriorityExposureDidNotSolveIt() {
        DiscountThenPriorityStrategy donating = new DiscountThenPriorityStrategy(10, 60, true);

        assertThat(donating.decide(exhausted(RecommendationType.DISCOUNT)).action())
                .isEqualTo(SurplusAction.PRIORITIZE);
        assertThat(donating.decide(exhausted(RecommendationType.SURPLUS_DESTINATION)).action())
                .isEqualTo(SurplusAction.DONATE);
    }
}
