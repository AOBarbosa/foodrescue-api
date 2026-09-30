package br.com.seudominio.foodrescue.business.event;

import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Announces that an establishment answered a discount recommendation (UC07).
 *
 * <p>Published for every answer, including a refusal, so a subscriber can
 * decide what each outcome means to it. UC08 is expected to create an offer
 * when {@code status} is {@link RecommendationStatus#ACCEPTED} or
 * {@link RecommendationStatus#ADJUSTED}.</p>
 *
 * @param recommendationId  the answered recommendation
 * @param productId         the discounted product
 * @param establishmentId   the establishment that answered
 * @param status            the resulting status
 * @param appliedPercentage the percentage actually applied, or {@code null} when refused
 * @param respondedAt       the moment of the answer
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record DiscountRecommendationRespondedEvent(
        Long recommendationId,
        Long productId,
        Long establishmentId,
        RecommendationStatus status,
        BigDecimal appliedPercentage,
        LocalDateTime respondedAt) {
}
