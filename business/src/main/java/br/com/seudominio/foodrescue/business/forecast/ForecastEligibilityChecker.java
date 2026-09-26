package br.com.seudominio.foodrescue.business.forecast;

import br.com.seudominio.foodrescue.domain.entities.Sale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Decides whether a product has enough sales history to be forecast (UC05).
 * The minimum number of past sales is configurable through
 * {@code app.forecast.min-sales} (defaults to 5).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class ForecastEligibilityChecker {

    private final int minimumSales;

    /**
     * Constructor.
     *
     * @param minimumSales the minimum number of past sales required to forecast
     */
    public ForecastEligibilityChecker(@Value("${app.forecast.min-sales:5}") int minimumSales) {
        this.minimumSales = minimumSales;
    }

    /**
     * Checks whether the given sales history is enough to forecast.
     *
     * @param salesHistory the product's past sales
     * @return {@code true} if the history has at least the minimum number of sales
     */
    public boolean isEligible(List<Sale> salesHistory) {
        return salesHistory.size() >= minimumSales;
    }

    /**
     * Returns the minimum number of past sales required to forecast.
     *
     * @return the minimum number of past sales
     */
    public int getMinimumSales() {
        return minimumSales;
    }
}
