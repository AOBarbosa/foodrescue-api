package br.com.seudominio.foodrescue.business.event;

/**
 * Announces that a product's stock reached zero (UC03/UC04).
 *
 * <p>Published whenever a sale or an inventory update leaves the product with
 * no units left. UC08 subscribes to close any offer still open for it, so an
 * offer can never stay active without stock to back it.</p>
 *
 * @param productId       the product that ran out
 * @param establishmentId the establishment that owns it
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record ProductStockDepletedEvent(Long productId, Long establishmentId) {
}
