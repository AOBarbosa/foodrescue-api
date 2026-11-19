package br.com.seudominio.perishrescue.business.forecast;

import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * The framework's default {@link ForecastHorizon}: forecast from now until the
 * resource's {@code criticalDeadline}, counting time in a configured unit
 * ({@code perishrescue.forecast.granularity}, {@code HOURS} by default).
 *
 * <p>Because the deadline itself is a hot spot of the resource (hot spot 1),
 * this default already fits every target application: a food item's
 * deadline is today's closing time, a medicine's is the batch expiration, a
 * flower's is the end of its aesthetic life.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public class DeadlineForecastHorizon implements ForecastHorizon {

    private final ChronoUnit granularity;

    /**
     * Creates the horizon.
     *
     * @param granularity the unit the remaining time is counted in
     */
    public DeadlineForecastHorizon(ChronoUnit granularity) {
        this.granularity = Objects.requireNonNull(granularity, "granularity");
    }

    @Override
    public ForecastWindow windowFor(PerishableResource resource, LocalDateTime now) {
        return new ForecastWindow(now, resource.criticalDeadline(now), granularity);
    }
}
