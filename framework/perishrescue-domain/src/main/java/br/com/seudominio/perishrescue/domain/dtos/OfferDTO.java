package br.com.seudominio.perishrescue.domain.dtos;

import br.com.seudominio.perishrescue.domain.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An offer of surplus published to beneficiaries (UC08).
 *
 * @param resourceId         the offered resource's identifier
 * @param resourceName       the offered resource's name
 * @param originalPrice     the resource's list price, shown as the "from" price
 * @param discountedPrice   the price the beneficiary pays
 * @param availableQuantity units still available in this offer
 * @param expiresAt         the moment the offer stops accepting orders
 * @param status            {@code ACTIVE} while it can be ordered, {@code CLOSED} otherwise
 * @param priority          whether the offer is highlighted (used from UC11 on)
 * @param id                the offer's identifier
 * @param createdAt         the moment the offer was created
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record OfferDTO(
        Long id,
        Long resourceId,
        String resourceName,
        BigDecimal originalPrice,
        BigDecimal discountedPrice,
        int availableQuantity,
        LocalDateTime expiresAt,
        OfferStatus status,
        boolean priority,
        LocalDateTime createdAt) {
}
