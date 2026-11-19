package br.com.seudominio.perishrescue.business.forecast;

import br.com.seudominio.perishrescue.domain.entities.Sale;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Input of a {@link DemandForecastStrategy}: what the strategy may consider
 * to estimate how many units of a resource will still sell within the
 * forecast window (UC05).
 *
 * @param resourceName        the resource's name
 * @param resourceCategory    the resource's category
 * @param salesHistory        the resource's past sales (lookback period)
 * @param currentStock        the units currently in stock
 * @param window              the period to forecast, from the instance's {@link ForecastHorizon}
 * @param businessDescription how the instance describes its business to an AI model
 *                            (e.g. "um pequeno estabelecimento de alimentos")
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record ForecastContext(
        String resourceName,
        String resourceCategory,
        List<Sale> salesHistory,
        int currentStock,
        ForecastWindow window,
        String businessDescription) {

    /**
     * The moment of the forecast (the start of the window).
     *
     * @return the current date and time
     */
    public LocalDateTime now() {
        return window.from();
    }

    /**
     * Returns a new builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ForecastContext}. The window is given either whole
     * ({@link #window}) or as {@link #now} + {@link #until} + {@link #granularity}
     * (hours by default).
     */
    public static class Builder {
        private String resourceName;
        private String resourceCategory;
        private List<Sale> salesHistory = List.of();
        private int currentStock;
        private ForecastWindow window;
        private LocalDateTime now;
        private LocalDateTime until;
        private ChronoUnit granularity = ChronoUnit.HOURS;
        private String businessDescription = "um estabelecimento que vende itens perecíveis";

        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder resourceCategory(String resourceCategory) {
            this.resourceCategory = resourceCategory;
            return this;
        }

        public Builder salesHistory(List<Sale> salesHistory) {
            this.salesHistory = salesHistory;
            return this;
        }

        public Builder currentStock(int currentStock) {
            this.currentStock = currentStock;
            return this;
        }

        public Builder window(ForecastWindow window) {
            this.window = window;
            return this;
        }

        public Builder now(LocalDateTime now) {
            this.now = now;
            return this;
        }

        public Builder until(LocalDateTime until) {
            this.until = until;
            return this;
        }

        public Builder granularity(ChronoUnit granularity) {
            this.granularity = granularity;
            return this;
        }

        public Builder businessDescription(String businessDescription) {
            this.businessDescription = businessDescription;
            return this;
        }

        public ForecastContext build() {
            ForecastWindow resolved = window != null ? window : new ForecastWindow(now, until, granularity);
            return new ForecastContext(resourceName, resourceCategory, salesHistory, currentStock, resolved,
                    businessDescription);
        }
    }
}
