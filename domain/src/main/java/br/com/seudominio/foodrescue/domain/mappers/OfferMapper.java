package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.dtos.OfferDTO;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.Product;

import org.springframework.stereotype.Component;

/**
 * Mapper between {@link Offer} and {@link OfferDTO}. Reused by UC09 and UC11,
 * which answer with the same shape.
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Component
public class OfferMapper implements DTOMapper<Offer, OfferDTO> {

    /**
     * Converts an {@link OfferDTO} to an {@link Offer}.
     * The product association is not resolved here.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity, or {@code null} if {@code dto} is {@code null}
     */
    @Override
    public Offer toEntity(OfferDTO dto) {
        if (dto == null) {
            return null;
        }

        return Offer.builder()
                .id(dto.id())
                .discountedPrice(dto.discountedPrice() == null ? null : Money.of(dto.discountedPrice()))
                .availableQuantity(dto.availableQuantity())
                .expiresAt(dto.expiresAt())
                .status(dto.status())
                .priority(dto.priority())
                .build();
    }

    /**
     * Converts an {@link Offer} to an {@link OfferDTO}.
     *
     * @param offer the entity to convert
     * @return the corresponding DTO, or {@code null} if {@code offer} is {@code null}
     */
    @Override
    public OfferDTO toDto(Offer offer) {
        if (offer == null) {
            return null;
        }

        Product product = offer.getProduct();
        Money originalPrice = product == null ? null : product.getOriginalPrice();

        return new OfferDTO(
                offer.getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                originalPrice == null ? null : originalPrice.amount(),
                offer.getDiscountedPrice() == null ? null : offer.getDiscountedPrice().amount(),
                offer.getAvailableQuantity(),
                offer.getExpiresAt(),
                offer.getStatus(),
                offer.isPriority(),
                offer.getCreationDate());
    }
}
