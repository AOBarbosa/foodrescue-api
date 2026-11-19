package br.com.seudominio.perishrescue.domain.dtos;

import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing a beneficiary's reservation of an offer (UC09).
 *
 * @param id             the reservation identifier
 * @param offerId        the reserved offer's identifier
 * @param resourceId     the offered resource's identifier
 * @param resourceName   the offered resource's name
 * @param beneficiaryId  the beneficiary's identifier
 * @param quantity       the reserved quantity
 * @param unitPrice      the unit price frozen when the reservation was made
 * @param totalPrice     the total reservation price ({@code unitPrice * quantity})
 * @param status         the lifecycle status ({@code RESERVED}, {@code RESCUED}, {@code EXPIRED}, {@code CANCELLED})
 * @param pickupCode     the code to show at pickup; only in the beneficiary's own views, {@code null} for the offerer
 * @param pickupDeadline the last moment the units can be picked up
 * @param rescuedAt      the moment the pickup was confirmed, if rescued
 * @param createdAt      the moment the reservation was created
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ReservationDTO(
        Long id,
        Long offerId,
        Long resourceId,
        String resourceName,
        Long beneficiaryId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        ReservationStatus status,
        String pickupCode,
        LocalDateTime pickupDeadline,
        LocalDateTime rescuedAt,
        LocalDateTime createdAt) {

    /**
     * The same reservation without the pickup code, for the offerer's views.
     *
     * @return a copy with {@code pickupCode} set to {@code null}
     */
    public ReservationDTO withoutPickupCode() {
        return new ReservationDTO(id, offerId, resourceId, resourceName, beneficiaryId, quantity, unitPrice,
                totalPrice, status, null, pickupDeadline, rescuedAt, createdAt);
    }
}
