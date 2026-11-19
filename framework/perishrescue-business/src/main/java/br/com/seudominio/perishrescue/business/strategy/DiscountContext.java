package br.com.seudominio.perishrescue.business.strategy;

import br.com.seudominio.perishrescue.business.forecast.ForecastWindow;
import br.com.seudominio.perishrescue.core.percentage.Percentage;

/**
 * Input of a {@link DiscountRecommendationStrategy}: everything the formula is
 * allowed to consider when suggesting a discount (UC07).
 *
 * @param riskPercentage      the waste risk calculated by UC06
 * @param stockQuantity       the units currently in stock
 * @param expectedSurplus     the units expected to be left over at the end of the window
 * @param window              the forecast window (hot spot 4): how much time is left, in the instance's unit
 * @param daysUntilExpiration days left until the resource expires, or {@code null} if it has no expiration date
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record DiscountContext(
        Percentage riskPercentage,
        int stockQuantity,
        int expectedSurplus,
        ForecastWindow window,
        Long daysUntilExpiration) {
}
