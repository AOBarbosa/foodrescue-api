package br.com.seudominio.perishrescue.domain.dtos;

/**
 * Result of a resource inventory update.
 *
 * @param resource              the updated resource
 * @param expirationDateInPast whether the saved expiration date is before the current date
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record ResourceInventoryUpdateDTO(
        ResourceDTO resource,
        boolean expirationDateInPast) {
}
