package br.com.seudominio.foodrescue.domain.dtos;

import br.com.seudominio.foodrescue.domain.enums.RecommendationDecision;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * An establishment's answer to a pending discount recommendation (UC07).
 *
 * @param decision            accept, adjust or refuse
 * @param adjustedPercentage  the establishment's own percentage; required for
 *                            {@link RecommendationDecision#ADJUST} and ignored otherwise
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public record RespondToDiscountRecommendationRequest(
        @NotNull(message = "decision must not be null")
        RecommendationDecision decision,

        @DecimalMin(value = "0.0", message = "adjustedPercentage must not be negative")
        @DecimalMax(value = "100.0", message = "adjustedPercentage must not be greater than 100")
        BigDecimal adjustedPercentage) {
}
