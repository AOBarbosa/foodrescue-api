package br.com.seudominio.foodrescue.business.forecast;

import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

/**
 * Output of a {@link DemandForecastStrategy}.
 *
 * @param predictedQuantity units expected to be sold until the end of the business day
 * @param confidence        the forecast's confidence level
 * @param sampleSize        number of past sales that backed the calculation
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ForecastResult(int predictedQuantity, ForecastConfidence confidence, int sampleSize) {
}
