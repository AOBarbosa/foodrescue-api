package br.com.seudominio.perishrescue.domain.dtos;

import java.time.LocalDate;

/**
 * Request data for a partial resource inventory update.
 *
 * @param stockQuantity  the new stock quantity, when informed
 * @param expirationDate the new expiration date, when informed
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record UpdateResourceInventoryRequest(
        Integer stockQuantity,
        LocalDate expirationDate) {
}
