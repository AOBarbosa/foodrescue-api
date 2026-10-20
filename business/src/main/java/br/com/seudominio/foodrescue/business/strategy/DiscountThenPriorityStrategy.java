package br.com.seudominio.foodrescue.business.strategy;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Initial {@link SurplusDestinationStrategy}: deepen the discount by a fixed
 * step while there is headroom under the maximum discount; once there is not,
 * make the product available with priority.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class DiscountThenPriorityStrategy implements SurplusDestinationStrategy {

    private final BigDecimal step;
    private final BigDecimal maxDiscount;

    /**
     * Constructor.
     *
     * @param step        percentage points added to the current discount
     * @param maxDiscount ceiling for the total discount
     */
    public DiscountThenPriorityStrategy(
            @Value("${app.surplus.discount-step:10}") double step,
            @Value("${app.discount.max-percentage:60}") double maxDiscount) {
        this.step = Percentage.of(step).value();
        this.maxDiscount = Percentage.of(maxDiscount).value();
    }

    @Override
    public SurplusDecision decide(SurplusContext context) {
        BigDecimal current = context.currentDiscount().value();
        BigDecimal headroom = maxDiscount.subtract(current);

        if (headroom.signum() <= 0) {
            return SurplusDecision.prioritize();
        }

        BigDecimal total = current.add(step).min(maxDiscount).setScale(2, RoundingMode.HALF_UP);
        return SurplusDecision.additionalDiscount(Percentage.of(total));
    }
}
