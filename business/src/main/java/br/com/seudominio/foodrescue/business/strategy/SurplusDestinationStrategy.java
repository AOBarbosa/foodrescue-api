package br.com.seudominio.foodrescue.business.strategy;

/**
 * Decides what to recommend for a product that is still at risk of being
 * wasted even after a discount: a deeper discount or priority exposure to
 * consumers (UC10).
 *
 * <p>Keeping the decision behind this interface lets new actions be added
 * without touching the service that reassesses the risk.</p>
 *
 * <p>Implementations must honor the same contract: the decision is never
 * {@code null}, and an {@link SurplusAction#ADDITIONAL_DISCOUNT} carries a
 * total percentage strictly above the product's current discount and not
 * above the configured maximum.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public interface SurplusDestinationStrategy {

    /**
     * Decides the alternative action for a product still at risk.
     *
     * @param context the reassessed risk, stock, remaining time and previous action
     * @return the recommended action
     */
    SurplusDecision decide(SurplusContext context);
}
