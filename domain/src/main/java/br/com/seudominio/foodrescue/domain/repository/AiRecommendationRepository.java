package br.com.seudominio.foodrescue.domain.repository;

import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;

import java.util.Optional;

/**
 * Port for persisting and retrieving {@link AiRecommendation} aggregates.
 * Kept minimal on purpose (ISP) — UC06/UC07/UC10 (issues #7/#8/#12) add the
 * finder methods (e.g. by product) that their own use cases actually need.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public interface AiRecommendationRepository {

    /**
     * Persists a recommendation, either creating or updating it.
     *
     * @param aiRecommendation the recommendation to persist
     * @return the persisted recommendation
     */
    AiRecommendation save(AiRecommendation aiRecommendation);

    /**
     * Finds a recommendation by its identifier.
     *
     * @param id the recommendation's identifier
     * @return the recommendation, or empty if not found
     */
    Optional<AiRecommendation> findById(Long id);
}
