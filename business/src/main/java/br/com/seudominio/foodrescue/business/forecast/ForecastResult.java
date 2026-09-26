package br.com.seudominio.foodrescue.business.forecast;

import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

/**
 * Output of a {@link DemandForecastStrategy}.
 *
 * @param predictedQuantity units expected to be sold until the end of the business day
 * @param confidence        the forecast's confidence level
 * @param sampleSize        number of past sales that backed the calculation
 * @param source            identifier of the algorithm or AI model that produced the forecast
 * @param rationale         short explanation of the forecast, if the source provides one
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ForecastResult(
        int predictedQuantity,
        ForecastConfidence confidence,
        int sampleSize,
        String source,
        String rationale) {
}
