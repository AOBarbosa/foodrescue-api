package br.com.seudominio.perishrescue.business.rescue;

import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * What a {@link RescueRule} can base its decision on.
 *
 * @param offer       the offer being reserved
 * @param beneficiary the beneficiary reserving
 * @param quantity    the requested quantity
 * @param now         the current date and time
 * @param attributes  extra data sent with the reservation request, for
 *                    instance-specific rules (e.g. a declared transport type);
 *                    never {@code null}
 * @param history     read-only access to past reservations
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record RescueContext(
        Offer offer,
        Beneficiary beneficiary,
        int quantity,
        LocalDateTime now,
        Map<String, Object> attributes,
        ReservationHistory history) {

    /**
     * Compact constructor: copies the attributes and never keeps {@code null}.
     */
    public RescueContext {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
