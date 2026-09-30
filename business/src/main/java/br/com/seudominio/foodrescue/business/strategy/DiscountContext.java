package br.com.seudominio.foodrescue.business.strategy;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

/**
 * Input of a {@link DiscountRecommendationStrategy}: everything the formula is
 * allowed to consider when suggesting a discount (UC07).
 *
 * @param riskPercentage      the waste risk calculated by UC06
 * @param stockQuantity       the units currently in stock
 * @param expectedSurplus     the units expected to be left over at closing time
 * @param hoursUntilClosing   hours left until the end of the business day, negative once it has passed
 * @param daysUntilExpiration days left until the product expires, or {@code null} if it has no expiration date
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record DiscountContext(
        Percentage riskPercentage,
        int stockQuantity,
        int expectedSurplus,
        long hoursUntilClosing,
        Long daysUntilExpiration) {
}
