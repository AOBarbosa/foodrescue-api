package br.com.seudominio.foodrescue.domain.dtos;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Consolidated impact and waste-reduction indicators for an establishment (UC12).
 *
 * @param period                  the period these metrics refer to
 * @param wasteAvoidedUnits       total units of products sold with discount, avoiding waste
 * @param recoveredRevenue        total revenue collected from discounted sales (revenue that would otherwise be lost)
 * @param acceptedRecommendations total discount recommendations accepted (including adjusted ones)
 * @param refusedRecommendations  total discount recommendations refused
 * @param adjustedRecommendations total discount recommendations where the establishment adjusted the percentage
 * @param comparisonPeriod        the indicators for the comparison period, or {@code null} if comparison was not requested
 *
 * @author Clovis Luan
 * @since 1.0.0
 */
public record WasteAndSavingsIndicatorsDTO(
        PeriodDTO period,
        int wasteAvoidedUnits,
        BigDecimal recoveredRevenue,
        long acceptedRecommendations,
        long refusedRecommendations,
        long adjustedRecommendations,
        WasteAndSavingsIndicatorsDTO comparisonPeriod) {

    private static final int SCALE = 2;

    /**
     * Compact constructor ensuring non-null recoveredRevenue with scale 2.
     */
    public WasteAndSavingsIndicatorsDTO {
        recoveredRevenue = recoveredRevenue != null
                ? recoveredRevenue.setScale(SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Returns a new builder instance.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent builder for {@link WasteAndSavingsIndicatorsDTO}.
     */
    public static class Builder {
        private PeriodDTO period;
        private int wasteAvoidedUnits;
        private BigDecimal recoveredRevenue;
        private long acceptedRecommendations;
        private long refusedRecommendations;
        private long adjustedRecommendations;
        private WasteAndSavingsIndicatorsDTO comparisonPeriod;

        public Builder period(PeriodDTO period) {
            this.period = period;
            return this;
        }

        public Builder wasteAvoidedUnits(int wasteAvoidedUnits) {
            this.wasteAvoidedUnits = wasteAvoidedUnits;
            return this;
        }

        public Builder recoveredRevenue(BigDecimal recoveredRevenue) {
            this.recoveredRevenue = recoveredRevenue;
            return this;
        }

        public Builder acceptedRecommendations(long acceptedRecommendations) {
            this.acceptedRecommendations = acceptedRecommendations;
            return this;
        }

        public Builder refusedRecommendations(long refusedRecommendations) {
            this.refusedRecommendations = refusedRecommendations;
            return this;
        }

        public Builder adjustedRecommendations(long adjustedRecommendations) {
            this.adjustedRecommendations = adjustedRecommendations;
            return this;
        }

        public Builder comparisonPeriod(WasteAndSavingsIndicatorsDTO comparisonPeriod) {
            this.comparisonPeriod = comparisonPeriod;
            return this;
        }

        public WasteAndSavingsIndicatorsDTO build() {
            BigDecimal revenue = recoveredRevenue != null
                    ? recoveredRevenue.setScale(SCALE, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

            return new WasteAndSavingsIndicatorsDTO(
                    period,
                    wasteAvoidedUnits,
                    revenue,
                    acceptedRecommendations,
                    refusedRecommendations,
                    adjustedRecommendations,
                    comparisonPeriod);
        }
    }
}
