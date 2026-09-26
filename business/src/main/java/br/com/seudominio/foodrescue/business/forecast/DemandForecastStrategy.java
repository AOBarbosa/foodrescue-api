package br.com.seudominio.foodrescue.business.forecast;

/**
 * Algorithm that estimates how many units of a product will still be sold
 * until the end of the business day (UC05).
 *
 * <p>Implementations must honor the same contract so they can replace each
 * other without changes to the calling service: given a {@link ForecastContext},
 * return a non-negative predicted quantity that never exceeds the current
 * stock, together with a confidence level.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public interface DemandForecastStrategy {

    /**
     * Calculates the demand forecast.
     *
     * @param context the sales history, current stock and forecast window
     * @return the predicted quantity and its confidence level
     */
    ForecastResult forecast(ForecastContext context);
}
