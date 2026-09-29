package br.com.seudominio.foodrescue.business.risk;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

/**
 * Formula that turns a product's stock and its forecast demand into a waste
 * risk percentage (UC06).
 *
 * <p>Keeping the formula behind this interface lets it be replaced (a more
 * elaborate model weighting the expiration date, for instance) without
 * touching the service that consumes it. UC10 reuses the same abstraction
 * when deciding whether a surplus is still at risk after a discount.</p>
 *
 * <p>Implementations must honor the same contract: the risk is
 * {@code 0%} whenever the stock is less than or equal to the predicted
 * demand, and grows with the expected surplus.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public interface RiskCalculator {

    /**
     * Calculates the waste risk of a product.
     *
     * @param stockQuantity     the units currently in stock
     * @param predictedQuantity the units expected to be sold until closing time
     * @return the waste risk percentage
     */
    Percentage calculate(int stockQuantity, int predictedQuantity);

    /**
     * Calculates the units expected to be left over at closing time.
     *
     * @param stockQuantity     the units currently in stock
     * @param predictedQuantity the units expected to be sold until closing time
     * @return the expected surplus, never negative
     */
    default int expectedSurplus(int stockQuantity, int predictedQuantity) {
        return Math.max(stockQuantity - predictedQuantity, 0);
    }
}
