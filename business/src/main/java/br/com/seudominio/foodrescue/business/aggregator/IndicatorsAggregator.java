package br.com.seudominio.foodrescue.business.aggregator;

import br.com.seudominio.foodrescue.domain.dtos.PeriodDTO;
import br.com.seudominio.foodrescue.domain.dtos.WasteAndSavingsIndicatorsDTO;
import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Aggregates recorded sales and AI discount recommendations into consolidated
 * impact and waste-reduction indicators (UC12).
 *
 * <p>Separated from {@code IndicatorsService} to honor Single Responsibility Principle (SRP).
 * If the period has no data, returns zeroed indicators rather than failing.</p>
 *
 * @author Clovis Luan
 * @since 1.0.0
 */
@Component
public class IndicatorsAggregator {

    private static final int SCALE = 2;

    /**
     * Aggregates sales and recommendations for a single period without comparison.
     *
     * @param period          the period definition
     * @param sales           the sales within the period
     * @param recommendations the recommendations responded within the period
     * @return the consolidated indicators DTO
     */
    public WasteAndSavingsIndicatorsDTO aggregate(
            PeriodDTO period,
            List<Sale> sales,
            List<AiRecommendation> recommendations) {
        return aggregate(period, sales, recommendations, null);
    }

    /**
     * Aggregates sales and recommendations for a period, optionally carrying comparison metrics.
     *
     * @param period           the period definition
     * @param sales            the sales within the period
     * @param recommendations  the recommendations responded within the period
     * @param comparisonPeriod the comparison period indicators, or {@code null}
     * @return the consolidated indicators DTO
     */
    public WasteAndSavingsIndicatorsDTO aggregate(
            PeriodDTO period,
            List<Sale> sales,
            List<AiRecommendation> recommendations,
            WasteAndSavingsIndicatorsDTO comparisonPeriod) {

        int wasteAvoidedUnits = 0;
        BigDecimal recoveredRevenue = BigDecimal.ZERO;

        if (sales != null) {
            for (Sale sale : sales) {
                Product product = sale.getProduct();
                if (product != null && product.getOriginalPrice() != null && sale.getUnitPrice() != null) {
                    if (product.getOriginalPrice().isGreaterThan(sale.getUnitPrice())) {
                        wasteAvoidedUnits += sale.getQuantity();
                        BigDecimal saleTotal = sale.getUnitPrice().amount()
                                .multiply(BigDecimal.valueOf(sale.getQuantity()));
                        recoveredRevenue = recoveredRevenue.add(saleTotal);
                    }
                }
            }
        }

        long acceptedCount = 0;
        long refusedCount = 0;
        long adjustedCount = 0;

        if (recommendations != null) {
            for (AiRecommendation recommendation : recommendations) {
                if (recommendation.getStatus() == RecommendationStatus.ACCEPTED) {
                    acceptedCount++;
                } else if (recommendation.getStatus() == RecommendationStatus.ADJUSTED) {
                    adjustedCount++;
                    acceptedCount++;
                } else if (recommendation.getStatus() == RecommendationStatus.REFUSED) {
                    refusedCount++;
                }
            }
        }

        return WasteAndSavingsIndicatorsDTO.builder()
                .period(period)
                .wasteAvoidedUnits(wasteAvoidedUnits)
                .recoveredRevenue(recoveredRevenue.setScale(SCALE, RoundingMode.HALF_UP))
                .acceptedRecommendations(acceptedCount)
                .refusedRecommendations(refusedCount)
                .adjustedRecommendations(adjustedCount)
                .comparisonPeriod(comparisonPeriod)
                .build();
    }
}
