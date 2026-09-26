package br.com.seudominio.foodrescue.domain.dtos;

import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

import java.time.LocalDateTime;

/**
 * Response carrying a product's demand forecast (UC05).
 *
 * @param id                the forecast's identifier
 * @param productId         the forecast product's identifier
 * @param predictedQuantity units expected to be sold until the end of the business day
 * @param stockQuantity     the product's stock quantity at the moment of calculation
 * @param confidence        the forecast's confidence level
 * @param sampleSize        number of past sales considered in the calculation
 * @param calculatedAt      the moment the forecast was calculated
 * @param forecastUntil     the end of the business day the forecast refers to
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record DemandForecastResponse(
        Long id,
        Long productId,
        int predictedQuantity,
        int stockQuantity,
        ForecastConfidence confidence,
        int sampleSize,
        LocalDateTime calculatedAt,
        LocalDateTime forecastUntil) {
}
