package br.com.seudominio.foodrescue.domain.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request to publish an offer of surplus manually (UC08).
 *
 * <p>Only the product is required. Every other field falls back to a sensible
 * default taken from the product itself, the same way {@code RegisterSaleDTO}
 * lets the unit price fall back to the product's current price in UC04.</p>
 *
 * @param productId         the product to offer
 * @param discountedPrice   the price the consumer pays; defaults to the product's current price
 * @param availableQuantity units to offer; defaults to the product's whole stock
 * @param expiresAt         when the offer stops accepting orders; defaults to today's closing time
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record CreateOfferRequest(
        @NotNull(message = "productId must not be null")
        Long productId,

        @Positive(message = "discountedPrice must be greater than zero")
        BigDecimal discountedPrice,

        @Positive(message = "availableQuantity must be greater than zero")
        Integer availableQuantity,

        LocalDateTime expiresAt) {
}
