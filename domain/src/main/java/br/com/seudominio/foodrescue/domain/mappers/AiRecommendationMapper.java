package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.entities.Product;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Mapper between {@link AiRecommendation} and {@link DiscountRecommendationDTO}.
 * Reused by UC10 and UC12, which answer with the same shape.
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Component
public class AiRecommendationMapper implements DTOMapper<AiRecommendation, DiscountRecommendationDTO> {

    /**
     * Converts a {@link DiscountRecommendationDTO} to an {@link AiRecommendation}.
     * The product association is not resolved here.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity, or {@code null} if {@code dto} is {@code null}
     */
    @Override
    public AiRecommendation toEntity(DiscountRecommendationDTO dto) {
        if (dto == null) {
            return null;
        }

        return AiRecommendation.builder()
                .id(dto.id())
                .type(dto.type())
                .suggestedPercentage(dto.suggestedPercentage() == null
                        ? null
                        : Percentage.of(dto.suggestedPercentage()))
                .status(dto.status())
                .respondedAt(dto.respondedAt())
                .build();
    }

    /**
     * Converts an {@link AiRecommendation} to a {@link DiscountRecommendationDTO},
     * without the expiration moment, which is a business policy the mapper does
     * not know about.
     *
     * @param recommendation the entity to convert
     * @return the corresponding DTO, or {@code null} if {@code recommendation} is {@code null}
     */
    @Override
    public DiscountRecommendationDTO toDto(AiRecommendation recommendation) {
        return toDto(recommendation, null);
    }

    /**
     * Converts an {@link AiRecommendation} to a {@link DiscountRecommendationDTO},
     * carrying the moment it stops accepting an answer.
     *
     * @param recommendation the entity to convert
     * @param expiresAt      the moment the recommendation expires, or {@code null} if not applicable
     * @return the corresponding DTO, or {@code null} if {@code recommendation} is {@code null}
     */
    public DiscountRecommendationDTO toDto(AiRecommendation recommendation, LocalDateTime expiresAt) {
        if (recommendation == null) {
            return null;
        }

        Product product = recommendation.getProduct();
        Percentage percentage = recommendation.getSuggestedPercentage();
        Money originalPrice = product == null ? null : product.getOriginalPrice();

        return new DiscountRecommendationDTO(
                recommendation.getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                recommendation.getType(),
                percentage == null ? null : percentage.value(),
                recommendation.getStatus(),
                originalPrice == null ? null : originalPrice.amount(),
                product == null || product.getCurrentPrice() == null ? null : product.getCurrentPrice().amount(),
                originalPrice == null || percentage == null
                        ? null
                        : originalPrice.applyDiscount(percentage).amount(),
                recommendation.getCreationDate(),
                expiresAt,
                recommendation.getRespondedAt());
    }
}
