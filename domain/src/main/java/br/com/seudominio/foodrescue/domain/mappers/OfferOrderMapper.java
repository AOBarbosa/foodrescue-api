package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.OfferOrder;
import br.com.seudominio.foodrescue.domain.entities.Product;

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
     * The offer and consumer associations are not resolved here.
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
        Product product = offer == null ? null : offer.getProduct();
        Consumer consumer = order.getConsumer();

        BigDecimal unitPrice = (offer != null && offer.getDiscountedPrice() != null)
                ? offer.getDiscountedPrice().amount()
                : null;
        BigDecimal totalPrice = unitPrice != null
                ? unitPrice.multiply(BigDecimal.valueOf(order.getQuantity()))
                : null;

        return new OfferOrderDTO(
                order.getId(),
                offer == null ? null : offer.getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                consumer == null ? null : consumer.getId(),
                order.getQuantity(),
                unitPrice,
                totalPrice,
                order.getStatus(),
                order.getCreationDate());
    }
}
