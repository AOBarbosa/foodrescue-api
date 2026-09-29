package br.com.seudominio.foodrescue.domain.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request payload for recording a sale.
 *
 * @param productId the identifier of the product being sold
 * @param quantity  the quantity sold (must be > 0)
 * @param unitPrice optional unit price (defaults to product current price if omitted)
 * @param soldAt    optional timestamp when the sale took place (defaults to now if omitted)
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record RegisterSaleDTO(
        @NotNull(message = "productId must not be null")
        Long productId,

        @NotNull(message = "quantity must not be null")
        @Positive(message = "quantity must be greater than zero")
        Integer quantity,

        @Positive(message = "unitPrice must be greater than zero")
        BigDecimal unitPrice,

        LocalDateTime soldAt
) {
}
