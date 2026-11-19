package br.com.seudominio.perishrescue.business.forecast;

import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import java.time.LocalDateTime;

/**
 * Until when, and in which unit, the kernel forecasts a resource's demand.
 *
 * <p><b>Hot spot 4 of the PerishRescue framework.</b> The kernel's
 * monitoring flow is fixed (sales history &rarr; demand forecast &rarr; waste
 * risk &rarr; recommendation &rarr; indicators); what varies per instance is
 * the horizon: until closing today (food), until the batch expires
 * (medicines), until the flower wilts (flowers). The framework's default,
 * {@link DeadlineForecastHorizon}, forecasts until the resource's
 * {@code criticalDeadline} in a configurable unit; an instance replaces it
 * by declaring its own {@code ForecastHorizon} bean.</p>
 *
 * <p>Instances also tune the rest of the hot spot through the
 * {@link DemandForecastStrategy} (how to forecast) and the
 * {@code perishrescue.forecast.business-description} property (how the
 * business is described to the AI model).</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public interface ForecastHorizon {

    /**
     * The window to forecast a resource's demand for, starting now.
     *
     * @param resource the resource
     * @param now      the current date and time
     * @return the forecast window
     */
    ForecastWindow windowFor(PerishableResource resource, LocalDateTime now);
}
