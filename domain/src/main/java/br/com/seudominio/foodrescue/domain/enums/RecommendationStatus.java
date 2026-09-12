package br.com.seudominio.foodrescue.domain.enums;

import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;

/**
 * Lifecycle status of an {@link AiRecommendation}.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public enum RecommendationStatus {

    PENDING,
    ACCEPTED,
    ADJUSTED,
    REFUSED,
    EXPIRED
}
