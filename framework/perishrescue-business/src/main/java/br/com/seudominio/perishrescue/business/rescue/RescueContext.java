package br.com.seudominio.perishrescue.business.rescue;

import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
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
     * Compact constructor: keeps a read-only copy of the attributes, never
     * {@code null}. Attributes with a {@code null} value (e.g. {@code {"transport": null}}
     * in the request) are dropped, as if they had not been sent.
     */
    public RescueContext {
        Map<String, Object> copy = new LinkedHashMap<>();
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (key != null && value != null) {
                    copy.put(key, value);
                }
            });
        }
        attributes = Collections.unmodifiableMap(copy);
    }
}
