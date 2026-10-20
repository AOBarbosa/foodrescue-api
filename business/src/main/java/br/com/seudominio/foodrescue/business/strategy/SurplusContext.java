package br.com.seudominio.foodrescue.business.strategy;

import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;

/**
 * Input of a {@link SurplusDestinationStrategy}: everything the decision is
 * allowed to consider when a product is still at risk after a discount (UC10).
 *
 * @param riskPercentage      the waste risk, reassessed by the same calculator of UC06
 * @param stockQuantity       the units currently in stock
 * @param expectedSurplus     the units expected to be left over at closing time
 * @param hoursUntilClosing   hours left until the end of the business day
 * @param currentDiscount     the discount the product's current price already carries
 * @param previousType        what the previous recommendation of the chain suggested
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record SurplusContext(
        Percentage riskPercentage,
        int stockQuantity,
        int expectedSurplus,
        long hoursUntilClosing,
        Percentage currentDiscount,
        RecommendationType previousType) {
}
