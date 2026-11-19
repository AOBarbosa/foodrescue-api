package br.com.seudominio.perishrescue.domain.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Map;

/**
 * Request to reserve units of an offer (UC09).
 *
 * @param offerId    the offer to reserve
 * @param quantity   how many units
 * @param attributes optional extra data for the instance's rescue rules
 *                   (e.g. {@code {"transport": "REFRIGERATED"}}); may be omitted
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ReserveOfferRequest(
        @NotNull(message = "offerId must not be null")
        Long offerId,

        @NotNull(message = "quantity must not be null")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity,

        Map<String, Object> attributes) {

    /**
     * Compact constructor: never keeps {@code null} attributes.
     */
    public ReserveOfferRequest {
        attributes = attributes == null ? Map.of() : attributes;
    }

    /**
     * Request without extra attributes.
     *
     * @param offerId  the offer to reserve
     * @param quantity how many units
     */
    public ReserveOfferRequest(Long offerId, Integer quantity) {
        this(offerId, quantity, Map.of());
    }
}
