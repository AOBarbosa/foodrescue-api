package br.com.seudominio.perishrescue.domain.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Request, sent by the offerer, to confirm that a beneficiary picked up a
 * reservation.
 *
 * @param pickupCode the code the beneficiary showed at the counter
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record ConfirmPickupRequest(@NotBlank String pickupCode) {
}
