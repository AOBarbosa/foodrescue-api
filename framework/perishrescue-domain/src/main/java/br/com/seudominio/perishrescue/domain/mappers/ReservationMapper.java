package br.com.seudominio.perishrescue.domain.mappers;

import br.com.seudominio.perishrescue.domain.dtos.ReservationDTO;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.Reservation;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper between {@link Reservation} and {@link ReservationDTO} (UC09).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class ReservationMapper implements DTOMapper<Reservation, ReservationDTO> {

    /**
     * Converts an {@link ReservationDTO} to an {@link Reservation}.
     * The offer and beneficiary associations are not resolved here.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity, or {@code null} if {@code dto} is {@code null}
     */
    @Override
    public Reservation toEntity(ReservationDTO dto) {
        if (dto == null) {
            return null;
        }

        return Reservation.builder()
                .id(dto.id())
                .quantity(dto.quantity())
                .status(dto.status())
                .build();
    }

    /**
     * Converts an {@link Reservation} to an {@link ReservationDTO}.
     *
     * @param order the entity to convert
     * @return the corresponding DTO, or {@code null} if {@code order} is {@code null}
     */
    @Override
    public ReservationDTO toDto(Reservation order) {
        if (order == null) {
            return null;
        }

        Offer offer = order.getOffer();
        PerishableResource resource = offer == null ? null : offer.getResource();
        Beneficiary beneficiary = order.getBeneficiary();

        BigDecimal unitPrice = order.getUnitPrice() != null
                ? order.getUnitPrice().amount()
                : (offer != null && offer.getDiscountedPrice() != null ? offer.getDiscountedPrice().amount() : null);
        BigDecimal totalPrice = unitPrice != null
                ? unitPrice.multiply(BigDecimal.valueOf(order.getQuantity()))
                : null;

        return new ReservationDTO(
                order.getId(),
                offer == null ? null : offer.getId(),
                resource == null ? null : resource.getId(),
                resource == null ? null : resource.getName(),
                beneficiary == null ? null : beneficiary.getId(),
                order.getQuantity(),
                unitPrice,
                totalPrice,
                order.getStatus(),
                order.getPickupCode(),
                order.getPickupDeadline(),
                order.getRescuedAt(),
                order.getCreationDate());
    }
}
