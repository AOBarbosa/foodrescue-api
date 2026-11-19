package br.com.seudominio.perishrescue.business.forecast;

import br.com.seudominio.perishrescue.domain.entities.Sale;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Input handed to a {@link DemandForecastStrategy}: the resource's
 * description, sales history, current stock and the forecast window (from
 * {@code now} until {@code closingAt}).
 *
 * @param resourceName     the resource's name
 * @param resourceCategory the resource's category
 * @param salesHistory the resource's past sales, oldest first
 * @param currentStock the resource's current stock quantity
 * @param now          the moment the forecast is calculated
 * @param closingAt    the end of the business day
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ForecastContext(
        String resourceName,
        String resourceCategory,
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

        private String resourceName;
        private String resourceCategory;
        private List<Sale> salesHistory = List.of();
        private int currentStock;
        private LocalDateTime now;
        private LocalDateTime closingAt;

        /**
         * Sets the resource's name.
         *
         * @param resourceName the resource's name
         * @return the current instance of {@link Builder}
         */
        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        /**
         * Sets the resource's category.
         *
         * @param resourceCategory the resource's category
         * @return the current instance of {@link Builder}
         */
        public Builder resourceCategory(String resourceCategory) {
            this.resourceCategory = resourceCategory;
            return this;
        }

        /**
         * Sets the resource's past sales.
         *
         * @param salesHistory the resource's past sales, oldest first
         * @return the current instance of {@link Builder}
         */
        public Builder salesHistory(List<Sale> salesHistory) {
            this.salesHistory = salesHistory;
            return this;
        }

        /**
         * Sets the resource's current stock quantity.
         *
         * @param currentStock the resource's current stock quantity
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
            return new ForecastContext(resourceName, resourceCategory, salesHistory, currentStock, now, closingAt);
        }
    }
}
