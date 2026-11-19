package br.com.seudominio.perishrescue.business.forecast;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * The period a demand forecast covers and the unit its time is measured in,
 * as defined by the instance's {@link ForecastHorizon} (hot spot 4).
 *
 * <p>FoodRescue forecasts until the offerer closes today, in hours;
 * PharmaRescue would forecast until the batch expires, in days. Everything in
 * the kernel that used to assume "until closing today" (the forecast, the
 * urgency of a discount, the surplus reassessment) reads the remaining time
 * from this window instead.</p>
 *
 * @param from        the start of the window (the moment of the forecast)
 * @param until       the end of the window (usually the resource's critical deadline)
 * @param granularity the unit the remaining time is counted in ({@code HOURS} or {@code DAYS})
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record ForecastWindow(LocalDateTime from, LocalDateTime until, ChronoUnit granularity) {

    /**
     * Compact constructor: all fields are required.
     */
    public ForecastWindow {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(until, "until");
        Objects.requireNonNull(granularity, "granularity");
    }

    /**
     * The time left in the window; negative once it has ended.
     *
     * @return the remaining duration
     */
    public Duration remaining() {
        return Duration.between(from, until);
    }

    /**
     * The time left in the window counted in whole {@link #granularity} units;
     * negative once it has ended.
     *
     * @return the remaining units
     */
    public long remainingUnits() {
        return granularity.between(from, until);
    }

    /**
     * Whether the window has already ended.
     *
     * @return {@code true} if {@code until} is not after {@code from}
     */
    public boolean isOver() {
        return !until.isAfter(from);
    }

    /**
     * Whether the whole window falls within a single calendar day (e.g.
     * "from now until closing today").
     *
     * @return {@code true} if {@code from} and {@code until} are on the same date
     */
    public boolean isSameDay() {
        return from.toLocalDate().equals(until.toLocalDate());
    }

    /**
     * Whether at most {@code amount} {@link #granularity} units are left.
     *
     * @param amount the threshold, in the window's unit
     * @return {@code true} if the remaining time is within the threshold
     */
    public boolean isWithin(long amount) {
        return remaining().compareTo(Duration.of(amount, granularity)) <= 0;
    }
}
