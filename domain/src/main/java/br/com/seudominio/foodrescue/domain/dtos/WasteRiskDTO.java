package br.com.seudominio.foodrescue.domain.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Waste risk of a product, derived from its latest demand forecast (UC05) and
 * its current stock (UC06).
 *
 * @param productId          the assessed product's identifier
 * @param productName        the assessed product's name
 * @param stockQuantity      the units currently in stock
 * @param predictedQuantity  the units expected to be sold until closing time
 * @param expectedSurplus    the units expected to be left over at closing time
 * @param riskPercentage     the calculated waste risk, between 0 and 100
 * @param riskThreshold      the percentage above which a product is flagged
 * @param atRisk             whether the risk is above the threshold
 * @param forecastId         the forecast the assessment is based on
 * @param forecastCalculatedAt the moment that forecast was calculated
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record WasteRiskDTO(
        Long productId,
        String productName,
        int stockQuantity,
        int predictedQuantity,
        int expectedSurplus,
        BigDecimal riskPercentage,
        BigDecimal riskThreshold,
        boolean atRisk,
        Long forecastId,
        LocalDateTime forecastCalculatedAt) {
}
