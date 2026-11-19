package br.com.seudominio.perishrescue.domain.dtos;

import br.com.seudominio.perishrescue.domain.enums.OfferOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a beneficiary's reservation/purchase of an offer (UC09).
 *
 * @param id          the reservation identifier
 * @param offerId     the reserved offer's identifier
 * @param resourceId   the offered resource's identifier
 * @param resourceName the offered resource's name
 * @param beneficiaryId  the beneficiary's identifier
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
        Long resourceId,
        String resourceName,
        Long beneficiaryId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        OfferOrderStatus status,
        LocalDateTime createdAt) {
}
