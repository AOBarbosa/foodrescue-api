package br.com.seudominio.foodrescue.domain.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload for reserving/purchasing an active offer (UC09).
 *
 * @param offerId  the identifier of the offer being reserved
 * @param quantity the quantity desired by the consumer (must be greater than zero)
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ReserveOfferRequest(
        @NotNull(message = "offerId must not be null")
        Long offerId,

        @NotNull(message = "quantity must not be null")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity) {
}
