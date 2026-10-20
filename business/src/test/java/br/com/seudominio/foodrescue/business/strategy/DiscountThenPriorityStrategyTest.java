package br.com.seudominio.foodrescue.business.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;

import org.junit.jupiter.api.Test;

class DiscountThenPriorityStrategyTest {

    private final DiscountThenPriorityStrategy strategy = new DiscountThenPriorityStrategy(10, 60);

    private SurplusContext context(double currentDiscount) {
        return new SurplusContext(Percentage.of(80), 20, 16, 2, Percentage.of(currentDiscount),
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
}
