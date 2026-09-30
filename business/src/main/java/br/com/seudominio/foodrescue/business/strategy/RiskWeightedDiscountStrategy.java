package br.com.seudominio.foodrescue.business.strategy;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Initial {@link DiscountRecommendationStrategy}: the suggestion is a fraction
 * of the waste risk, raised by a fixed bonus when time is running out, and
 * capped by a configured maximum.
 *
 * <pre>
 * sugest&atilde;o = risco &times; fator [+ b&ocirc;nus de urg&ecirc;ncia], limitado ao m&aacute;ximo
 * </pre>
 *
 * <p>The three inputs the use case must weigh are covered as follows: the
 * <em>risk</em> drives the base discount, the <em>remaining time</em> (until
 * closing or expiration) drives the urgency bonus, and the <em>stock</em>
 * enters as a gate &mdash; a product with no expected surplus gets no discount
 * at all, regardless of its risk.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Component
public class RiskWeightedDiscountStrategy implements DiscountRecommendationStrategy {

    private static final int SCALE = 2;

    private final BigDecimal riskFactor;
    private final BigDecimal urgencyBonus;
    private final long urgentHours;
    private final BigDecimal maxDiscount;

    /**
     * Constructor.
     *
     * @param riskFactor   fraction of the risk that becomes discount (e.g. {@code 0.6} turns 80% risk into 48%)
     * @param urgencyBonus percentage points added when the product is running out of time
     * @param urgentHours  how many hours before closing the product is considered urgent
     * @param maxDiscount  ceiling for any suggestion, so the formula can never give the product away
     */
    public RiskWeightedDiscountStrategy(
            @Value("${app.discount.risk-factor:0.6}") double riskFactor,
            @Value("${app.discount.urgency-bonus:15}") double urgencyBonus,
            @Value("${app.discount.urgent-hours:4}") long urgentHours,
            @Value("${app.discount.max-percentage:60}") double maxDiscount) {
        this.riskFactor = BigDecimal.valueOf(riskFactor);
        this.urgencyBonus = BigDecimal.valueOf(urgencyBonus);
        this.urgentHours = urgentHours;
        this.maxDiscount = Percentage.of(maxDiscount).value();
    }

    @Override
    public Percentage suggest(DiscountContext context) {
        if (context.expectedSurplus() <= 0) {
            return Percentage.of(BigDecimal.ZERO);
        }

        BigDecimal suggested = context.riskPercentage().value().multiply(riskFactor);
        if (isUrgent(context)) {
            suggested = suggested.add(urgencyBonus);
        }

        return Percentage.of(suggested.min(maxDiscount).setScale(SCALE, RoundingMode.HALF_UP));
    }

    /**
     * Returns the ceiling applied to every suggestion.
     *
     * @return the maximum discount percentage
     */
    public Percentage getMaxDiscount() {
        return Percentage.of(maxDiscount);
    }

    private boolean isUrgent(DiscountContext context) {
        return context.hoursUntilClosing() <= urgentHours
                || (context.daysUntilExpiration() != null && context.daysUntilExpiration() <= 1);
    }
}
