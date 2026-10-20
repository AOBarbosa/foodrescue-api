package br.com.seudominio.foodrescue.domain.dtos;

import br.com.seudominio.foodrescue.domain.enums.OfferOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a consumer's reservation/purchase of an offer (UC09).
 *
 * @param id          the reservation identifier
 * @param offerId     the reserved offer's identifier
 * @param productId   the offered product's identifier
 * @param productName the offered product's name
 * @param consumerId  the consumer's identifier
 * @param quantity    the reserved quantity
 * @param unitPrice   the discounted unit price of the offer
 * @param totalPrice  the total reservation price ({@code unitPrice * quantity})
 * @param status      the lifecycle status of the reservation ({@code RESERVED} or {@code CANCELLED})
 * @param createdAt   the moment the reservation was created
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record OfferOrderDTO(
        Long id,
        Long offerId,
        Long productId,
        String productName,
        Long consumerId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        OfferOrderStatus status,
        LocalDateTime createdAt) {
}
