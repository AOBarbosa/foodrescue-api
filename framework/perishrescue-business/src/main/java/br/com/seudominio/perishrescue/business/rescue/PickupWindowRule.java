package br.com.seudominio.perishrescue.business.rescue;

import br.com.seudominio.perishrescue.core.validation.ValidationError;
import br.com.seudominio.perishrescue.domain.entities.Offerer;

import java.time.LocalTime;
import java.util.Optional;

/**
 * Reusable {@link RescueRule}: reservations are only taken while the offerer
 * is open, so the pickup can happen in person within business hours.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public class PickupWindowRule implements RescueRule {

    @Override
    public Optional<ValidationError> check(RescueContext context) {
        Offerer offerer = context.offer().getResource().getOfferer();
        LocalTime time = context.now().toLocalTime();
        boolean open = !time.isBefore(offerer.getOpeningTime()) && time.isBefore(offerer.getClosingTime());
        if (open) {
            return Optional.empty();
        }
        return Optional.of(new ValidationError(
                String.format("pickup only during business hours (%s-%s)",
                        offerer.getOpeningTime(), offerer.getClosingTime()),
                "offerId", context.offer().getId(), "OUTSIDE_BUSINESS_HOURS"));
    }
}
