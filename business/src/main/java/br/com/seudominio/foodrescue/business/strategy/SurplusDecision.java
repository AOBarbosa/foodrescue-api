package br.com.seudominio.foodrescue.business.strategy;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

/**
 * Outcome of a {@link SurplusDestinationStrategy}.
 *
 * @param action     the alternative action recommended
 * @param percentage the new total discount for {@link SurplusAction#ADDITIONAL_DISCOUNT},
 *                   {@code null} for {@link SurplusAction#PRIORITIZE}
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record SurplusDecision(SurplusAction action, Percentage percentage) {

    /**
     * Decision to deepen the discount.
     *
     * @param percentage the new total discount
     * @return the decision
     */
    public static SurplusDecision additionalDiscount(Percentage percentage) {
        return new SurplusDecision(SurplusAction.ADDITIONAL_DISCOUNT, percentage);
    }

    /**
     * Decision to make the product available to consumers with priority.
     *
     * @return the decision
     */
    public static SurplusDecision prioritize() {
        return new SurplusDecision(SurplusAction.PRIORITIZE, null);
    }
}
