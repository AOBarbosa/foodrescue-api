package br.com.seudominio.perishrescue.business.event;

/**
 * Announces that a resource's stock reached zero (UC03/UC04).
 *
 * <p>Published whenever a sale or an inventory update leaves the resource with
 * no units left. UC08 subscribes to close any offer still open for it, so an
 * offer can never stay active without stock to back it.</p>
 *
 * @param resourceId       the resource that ran out
 * @param offererId the offerer that owns it
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record ResourceStockDepletedEvent(Long resourceId, Long offererId) {
}
