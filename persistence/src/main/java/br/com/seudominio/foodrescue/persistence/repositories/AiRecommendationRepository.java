package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;

import org.springframework.stereotype.Repository;

/**
 * AI recommendation repository - This class is responsible for the database operations of the AI recommendation entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface AiRecommendationRepository extends GenericRepository<AiRecommendation> {
}
