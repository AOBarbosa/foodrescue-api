package br.com.seudominio.perishrescue.business.pricing;

import br.com.seudominio.perishrescue.core.percentage.Percentage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The framework's ready-made {@link PricingPolicy}: a table of time steps,
 * each with its discount.
 *
 * <p>The steps are measured either backwards from the resource's
 * {@code criticalDeadline} or forwards from its {@code lifecycleStart}, in
 * hours or days. That covers the three target applications with
 * configuration alone:</p>
 *
 * <ul>
 *   <li>FoodRescue: {@code UNTIL_DEADLINE}, hours, {@code 4 -> 30%, 1 -> 70%}
 *       (30% off from 4 hours before closing, 70% off from 1 hour before);</li>
 *   <li>PharmaRescue: {@code UNTIL_DEADLINE}, days, {@code 60 -> 30%, 15 -> 70%};</li>
 *   <li>FloraRescue: {@code SINCE_START}, days, {@code 1 -> 15%, 3 -> 50%, 5 -> 80%}.</li>
 * </ul>
 *
 * <p>With no steps it grants no discount, so an instance that configures
 * nothing keeps the offerer's own prices.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public class StepwiseDepreciationPolicy implements PricingPolicy {

    /**
     * Where the steps are measured from.
     */
    public enum TimeReference {
        /** Time left until the resource's critical deadline: the closer, the bigger the discount. */
        UNTIL_DEADLINE,
        /** Time elapsed since the resource's lifecycle start: the longer, the bigger the discount. */
        SINCE_START
    }

    private static final Percentage NONE = Percentage.of(0);

    private final TimeReference reference;
    private final ChronoUnit unit;
    private final NavigableMap<Long, Percentage> steps;

    /**
     * Creates the policy.
     *
     * @param reference where the steps are measured from
     * @param unit      the unit of the step thresholds ({@code HOURS} or {@code DAYS})
     * @param steps     threshold (in {@code unit}) to discount
     */
    public StepwiseDepreciationPolicy(TimeReference reference, ChronoUnit unit, Map<Long, Percentage> steps) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.unit = Objects.requireNonNull(unit, "unit");
        this.steps = new TreeMap<>(Objects.requireNonNull(steps, "steps"));
    }

    /**
     * Parses steps written as {@code "4:30,1:70"} (threshold:percent pairs).
     *
     * @param spec the steps, or blank for none
     * @return the parsed steps
     * @throws IllegalArgumentException if a pair is malformed
     */
    public static Map<Long, Percentage> parseSteps(String spec) {
        Map<Long, Percentage> parsed = new TreeMap<>();
        if (spec == null || spec.isBlank()) {
            return parsed;
        }
        for (String pair : spec.split(",")) {
            String[] parts = pair.trim().split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("invalid pricing step '" + pair + "', expected threshold:percent");
            }
            parsed.put(Long.parseLong(parts[0].trim()), Percentage.of(Double.parseDouble(parts[1].trim())));
        }
        return parsed;
    }

    @Override
    public Percentage discountFor(PricingContext context) {
        if (steps.isEmpty()) {
            return NONE;
        }
        LocalDateTime now = context.now();
        return switch (reference) {
            case UNTIL_DEADLINE -> untilDeadline(Duration.between(now, context.resource().criticalDeadline(now)));
            case SINCE_START -> sinceStart(context.resource().lifecycleStart() == null
                    ? Duration.ZERO
                    : Duration.between(context.resource().lifecycleStart(), now));
        };
    }

    /**
     * The step with the smallest threshold that the remaining time is still
     * within (e.g. with 4h and 1h steps, 3h left falls in the 4h step).
     */
    private Percentage untilDeadline(Duration remaining) {
        Percentage discount = NONE;
        for (Map.Entry<Long, Percentage> step : steps.descendingMap().entrySet()) {
            if (remaining.compareTo(Duration.of(step.getKey(), unit)) <= 0) {
                discount = step.getValue();
            }
        }
        return discount;
    }

    /**
     * The step with the largest threshold that the elapsed time has reached.
     */
    private Percentage sinceStart(Duration elapsed) {
        Percentage discount = NONE;
        for (Map.Entry<Long, Percentage> step : steps.entrySet()) {
            if (elapsed.compareTo(Duration.of(step.getKey(), unit)) >= 0) {
                discount = step.getValue();
            }
        }
        return discount;
    }
}
