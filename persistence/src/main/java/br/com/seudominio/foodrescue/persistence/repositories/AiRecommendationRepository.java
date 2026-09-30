package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI recommendation repository - This class is responsible for the database operations of the AI recommendation entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface AiRecommendationRepository extends GenericRepository<AiRecommendation> {

    /**
     * Finds the whole recommendation history of a product, newest first (UC07).
     *
     * @param productId the product id
     * @return every active recommendation made for the product
     */
    List<AiRecommendation> findByProductIdAndActiveTrueOrderByCreationDateDesc(Long productId);

    /**
     * Finds the recommendations of an establishment in a given status, oldest
     * first, so the establishment answers the ones that have waited longest (UC07).
     *
     * @param establishmentId the establishment id
     * @param status          the status to filter by
     * @return the matching recommendations
     */
    List<AiRecommendation> findByProductEstablishmentIdAndStatusAndActiveTrueOrderByCreationDateAsc(
            Long establishmentId, RecommendationStatus status);

    /**
     * Finds the most recent recommendation of a product in a given status, used
     * to stop a second pending recommendation from being created (UC07).
     *
     * @param productId the product id
     * @param status    the status to filter by
     * @return the most recent matching recommendation, if any
     */
    Optional<AiRecommendation> findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
            Long productId, RecommendationStatus status);
}
