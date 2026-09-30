package br.com.seudominio.foodrescue.domain.dtos;

import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A discount recommendation and the state of the product it refers to (UC07).
 *
 * @param id                   the recommendation's identifier
 * @param productId            the recommended product's identifier
 * @param productName          the recommended product's name
 * @param type                 the kind of recommendation
 * @param suggestedPercentage  the percentage this recommendation stands for; once adjusted, the
 *                             establishment's own value
 * @param status               the lifecycle status of the recommendation
 * @param originalPrice        the product's list price, which the discount is applied to
 * @param currentPrice         the product's price right now
 * @param priceWithDiscount    what the price becomes (or became) with this percentage applied
 * @param createdAt            the moment the recommendation was generated
 * @param expiresAt            the moment it stops accepting an answer, or {@code null} if unknown
 * @param respondedAt          the moment the establishment answered, or {@code null} while pending
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record DiscountRecommendationDTO(
        Long id,
        Long productId,
        String productName,
        RecommendationType type,
        BigDecimal suggestedPercentage,
        RecommendationStatus status,
        BigDecimal originalPrice,
        BigDecimal currentPrice,
        BigDecimal priceWithDiscount,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        LocalDateTime respondedAt) {
}
