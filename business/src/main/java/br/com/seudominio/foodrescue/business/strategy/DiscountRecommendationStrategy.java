package br.com.seudominio.foodrescue.business.strategy;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

/**
 * Formula that turns a product's waste risk, stock and remaining time into a
 * suggested discount percentage (UC07).
 *
 * <p>Keeping the formula behind this interface lets it be replaced &mdash; by a
 * model trained on accepted/refused recommendations, for instance &mdash; without
 * touching the service that creates or answers recommendations.</p>
 *
 * <p>Implementations must honor the same contract: a product with no expected
 * surplus gets {@code 0%}, and the suggestion never exceeds the configured
 * maximum discount.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public interface DiscountRecommendationStrategy {

    /**
     * Suggests a discount percentage for a product at risk of being wasted.
     *
     * @param context the waste risk, stock and remaining time
     * @return the suggested discount percentage
     */
    Percentage suggest(DiscountContext context);
}
