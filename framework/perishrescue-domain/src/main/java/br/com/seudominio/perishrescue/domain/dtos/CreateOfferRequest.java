package br.com.seudominio.perishrescue.domain.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request to publish an offer of surplus manually (UC08).
 *
 * <p>Only the resource is required. Every other field falls back to a sensible
 * default taken from the resource itself, the same way {@code RegisterSaleDTO}
 * lets the unit price fall back to the resource's current price in UC04.</p>
 *
 * @param resourceId         the resource to offer
 * @param discountedPrice   the price the beneficiary pays; defaults to the resource's current price
 * @param availableQuantity units to offer; defaults to the resource's whole stock
 * @param expiresAt         when the offer stops accepting orders; defaults to today's closing time
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record CreateOfferRequest(
        @NotNull(message = "resourceId must not be null")
        Long resourceId,

        @Positive(message = "discountedPrice must be greater than zero")
        BigDecimal discountedPrice,

        @Positive(message = "availableQuantity must be greater than zero")
        Integer availableQuantity,

        LocalDateTime expiresAt) {
}
