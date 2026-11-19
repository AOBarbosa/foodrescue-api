package br.com.seudominio.perishrescue.domain.mappers;

import br.com.seudominio.perishrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.OfferOrder;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Mapper between {@link OfferOrder} and {@link OfferOrderDTO} (UC09).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class OfferOrderMapper implements DTOMapper<OfferOrder, OfferOrderDTO> {

    /**
     * Converts an {@link OfferOrderDTO} to an {@link OfferOrder}.
     * The offer and beneficiary associations are not resolved here.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity, or {@code null} if {@code dto} is {@code null}
     */
    @Override
    public OfferOrder toEntity(OfferOrderDTO dto) {
        if (dto == null) {
            return null;
        }

        return OfferOrder.builder()
                .id(dto.id())
                .quantity(dto.quantity())
                .status(dto.status())
                .build();
    }

    /**
     * Converts an {@link OfferOrder} to an {@link OfferOrderDTO}.
     *
     * @param order the entity to convert
     * @return the corresponding DTO, or {@code null} if {@code order} is {@code null}
     */
    @Override
    public OfferOrderDTO toDto(OfferOrder order) {
        if (order == null) {
            return null;
        }

        Offer offer = order.getOffer();
        PerishableResource resource = offer == null ? null : offer.getResource();
        Beneficiary beneficiary = order.getBeneficiary();

        BigDecimal unitPrice = (offer != null && offer.getDiscountedPrice() != null)
                ? offer.getDiscountedPrice().amount()
                : null;
        BigDecimal totalPrice = unitPrice != null
                ? unitPrice.multiply(BigDecimal.valueOf(order.getQuantity()))
                : null;

        return new OfferOrderDTO(
                order.getId(),
                offer == null ? null : offer.getId(),
                resource == null ? null : resource.getId(),
                resource == null ? null : resource.getName(),
                beneficiary == null ? null : beneficiary.getId(),
                order.getQuantity(),
                unitPrice,
                totalPrice,
                order.getStatus(),
                order.getCreationDate());
    }
}
