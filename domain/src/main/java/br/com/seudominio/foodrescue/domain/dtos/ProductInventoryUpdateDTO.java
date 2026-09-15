package br.com.seudominio.foodrescue.domain.dtos;

/**
 * Result of a product inventory update.
 *
 * @param product              the updated product
 * @param expirationDateInPast whether the saved expiration date is before the current date
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record ProductInventoryUpdateDTO(
        ProductDTO product,
        boolean expirationDateInPast) {
}
