package br.com.seudominio.perishrescue.business.strategy;

import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Initial {@link SurplusDestinationStrategy}: deepen the discount by a fixed
 * step while there is headroom under the maximum discount; once there is not,
 * make the resource available with priority; and, if the instance enables
 * {@code app.surplus.donate-when-exhausted} (e.g. PharmaRescue), suggest a
 * donation when the priority exposure did not solve the surplus either.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class DiscountThenPriorityStrategy implements SurplusDestinationStrategy {

    private final BigDecimal step;
    private final BigDecimal maxDiscount;
    private final boolean donateWhenExhausted;

    /**
     * Constructor.
     *
     * @param step        percentage points added to the current discount
     * @param maxDiscount ceiling for the total discount
     */
    public DiscountThenPriorityStrategy(double step, double maxDiscount) {
        this(step, maxDiscount, false);
    }

    /**
     * Constructor.
     *
     * @param step                percentage points added to the current discount
     * @param maxDiscount         ceiling for the total discount
     * @param donateWhenExhausted whether to suggest a donation after a priority exposure
     */
    @Autowired
    public DiscountThenPriorityStrategy(
            @Value("${app.surplus.discount-step:10}") double step,
            @Value("${app.discount.max-percentage:60}") double maxDiscount,
            @Value("${app.surplus.donate-when-exhausted:false}") boolean donateWhenExhausted) {
        this.step = Percentage.of(step).value();
        this.maxDiscount = Percentage.of(maxDiscount).value();
        this.donateWhenExhausted = donateWhenExhausted;
    }

    @Override
    public SurplusDecision decide(SurplusContext context) {
        BigDecimal current = context.currentDiscount().value();
        BigDecimal headroom = maxDiscount.subtract(current);

        if (headroom.signum() <= 0) {
            boolean alreadyPrioritized = context.previousType() == RecommendationType.SURPLUS_DESTINATION;
            return donateWhenExhausted && alreadyPrioritized ? SurplusDecision.donate() : SurplusDecision.prioritize();
        }

        BigDecimal total = current.add(step).min(maxDiscount).setScale(2, RoundingMode.HALF_UP);
        return SurplusDecision.additionalDiscount(Percentage.of(total));
    }
}
