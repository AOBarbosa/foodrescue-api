package br.com.seudominio.perishrescue.business.event;

import br.com.seudominio.perishrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Announces that an offerer answered a discount recommendation (UC07).
 *
 * <p>Published for every answer, including a refusal, so a subscriber can
 * decide what each outcome means to it. UC08 is expected to create an offer
 * when {@code status} is {@link RecommendationStatus#ACCEPTED} or
 * {@link RecommendationStatus#ADJUSTED}.</p>
 *
 * @param recommendationId  the answered recommendation
 * @param resourceId         the discounted resource
 * @param offererId   the offerer that answered
 * @param status            the resulting status
 * @param appliedPercentage the percentage actually applied, or {@code null} when refused
 * @param recommendationType the kind of recommendation answered (DISCOUNT or SURPLUS_DESTINATION)
 * @param respondedAt       the moment of the answer
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record DiscountRecommendationRespondedEvent(
        Long recommendationId,
        Long resourceId,
        Long offererId,
        RecommendationStatus status,
        BigDecimal appliedPercentage,
        RecommendationType recommendationType,
        LocalDateTime respondedAt) {
}
