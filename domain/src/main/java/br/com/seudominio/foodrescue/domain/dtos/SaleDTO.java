package br.com.seudominio.foodrescue.domain.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a recorded sale.
 *
 * @param id           the sale identifier
 * @param productId    the product identifier
 * @param quantity     the quantity sold
 * @param unitPrice    the unit price at the time of sale
 * @param totalPrice   the calculated total price (quantity * unitPrice)
 * @param soldAt       the timestamp when the sale occurred
 * @param creationDate the timestamp when the record was created
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record SaleDTO(
        Long id,
        Long productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        LocalDateTime soldAt,
        LocalDateTime creationDate
) {
}
