package br.com.seudominio.foodrescue.business.forecast;

import br.com.seudominio.foodrescue.domain.entities.Sale;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Input handed to a {@link DemandForecastStrategy}: the product's sales
 * history, its current stock and the forecast window (from {@code now}
 * until {@code closingAt}).
 *
 * @param salesHistory the product's past sales, oldest first
 * @param currentStock the product's current stock quantity
 * @param now          the moment the forecast is calculated
 * @param closingAt    the end of the business day
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ForecastContext(
        List<Sale> salesHistory,
        int currentStock,
        LocalDateTime now,
        LocalDateTime closingAt) {

    /**
     * Returns a new instance of the Builder for building ForecastContext objects.
     *
     * @return A new Builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class for creating instances of {@link ForecastContext}.
     */
    public static class Builder {

        private List<Sale> salesHistory = List.of();
        private int currentStock;
        private LocalDateTime now;
        private LocalDateTime closingAt;

        /**
         * Sets the product's past sales.
         *
         * @param salesHistory the product's past sales, oldest first
         * @return the current instance of {@link Builder}
         */
        public Builder salesHistory(List<Sale> salesHistory) {
            this.salesHistory = salesHistory;
            return this;
        }

        /**
         * Sets the product's current stock quantity.
         *
         * @param currentStock the product's current stock quantity
         * @return the current instance of {@link Builder}
         */
        public Builder currentStock(int currentStock) {
            this.currentStock = currentStock;
            return this;
        }

        /**
         * Sets the moment the forecast is calculated.
         *
         * @param now the moment the forecast is calculated
         * @return the current instance of {@link Builder}
         */
        public Builder now(LocalDateTime now) {
            this.now = now;
            return this;
        }

        /**
         * Sets the end of the business day.
         *
         * @param closingAt the end of the business day
         * @return the current instance of {@link Builder}
         */
        public Builder closingAt(LocalDateTime closingAt) {
            this.closingAt = closingAt;
            return this;
        }

        /**
         * Builds and returns an instance of {@link ForecastContext} with the set properties.
         *
         * @return a new instance of {@link ForecastContext}
         */
        public ForecastContext build() {
            return new ForecastContext(salesHistory, currentStock, now, closingAt);
        }
    }
}
