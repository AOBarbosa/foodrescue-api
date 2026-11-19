package br.com.seudominio.perishrescue.business.rescue;

import br.com.seudominio.perishrescue.core.validation.ValidationError;

import java.util.Optional;

/**
 * Reusable {@link RescueRule}: a reservation must take at least
 * {@code minUnits} units (e.g. whole bunches or buckets in FloraRescue).
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public class MinimumLotRule implements RescueRule {

    private final int minUnits;

    /**
     * Creates the rule.
     *
     * @param minUnits the fewest units a reservation may take
     */
    public MinimumLotRule(int minUnits) {
        if (minUnits <= 0) {
            throw new IllegalArgumentException("minUnits must be positive");
        }
        this.minUnits = minUnits;
    }

    @Override
    public Optional<ValidationError> check(RescueContext context) {
        if (context.quantity() >= minUnits) {
            return Optional.empty();
        }
        return Optional.of(new ValidationError(
                String.format("reservations must take at least %d units", minUnits),
                "quantity", context.quantity(), "BELOW_MINIMUM_LOT"));
    }
}
