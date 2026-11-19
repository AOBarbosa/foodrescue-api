package br.com.seudominio.perishrescue.business.strategy;

import br.com.seudominio.perishrescue.core.percentage.Percentage;

/**
 * Outcome of a {@link SurplusDestinationStrategy}.
 *
 * @param action     the alternative action recommended
 * @param percentage the new total discount for {@link SurplusAction#ADDITIONAL_DISCOUNT},
 *                   {@code null} for {@link SurplusAction#PRIORITIZE} and {@link SurplusAction#DONATE}
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
     * Decision to make the resource available to beneficiaries with priority.
     *
     * @return the decision
     */
    public static SurplusDecision prioritize() {
        return new SurplusDecision(SurplusAction.PRIORITIZE, null);
    }

    /**
     * Decision to take the remaining units off sale and donate them.
     *
     * @return the decision
     */
    public static SurplusDecision donate() {
        return new SurplusDecision(SurplusAction.DONATE, null);
    }
}
