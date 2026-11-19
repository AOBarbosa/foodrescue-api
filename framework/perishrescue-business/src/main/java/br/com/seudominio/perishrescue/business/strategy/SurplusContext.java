package br.com.seudominio.perishrescue.business.strategy;

import br.com.seudominio.perishrescue.business.forecast.ForecastWindow;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;

/**
 * Input of a {@link SurplusDestinationStrategy}: everything the decision is
 * allowed to consider when a resource is still at risk after a discount (UC10).
 *
 * @param riskPercentage      the waste risk, reassessed by the same calculator of UC06
 * @param stockQuantity       the units currently in stock
 * @param expectedSurplus     the units expected to be left over at the end of the window
 * @param window              the forecast window (hot spot 4): how much time is left, in the instance's unit
 * @param currentDiscount     the discount the resource's current price already carries
 * @param previousType        what the previous recommendation of the chain suggested
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record SurplusContext(
        Percentage riskPercentage,
        int stockQuantity,
        int expectedSurplus,
        ForecastWindow window,
        Percentage currentDiscount,
        RecommendationType previousType) {
}
