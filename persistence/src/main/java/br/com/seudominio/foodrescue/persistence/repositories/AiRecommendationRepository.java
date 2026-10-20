package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDateTime;
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

    /**
     * Finds the active discount recommendations of an establishment responded within a period,
     * oldest first (UC12).
     *
     * <p>Filters by type {@code DISCOUNT} to avoid mixing with future surplus destination recommendations.</p>
     *
     * @param establishmentId the establishment identifier
     * @param start           the start timestamp of the period (inclusive)
     * @param end             the end timestamp of the period (exclusive)
     * @return the discount recommendations responded within the period
     */
    @Query("SELECT r FROM AiRecommendation r JOIN r.product p WHERE p.establishment.id = :establishmentId "
            + "AND r.active = true AND r.respondedAt >= :start AND r.respondedAt < :end "
            + "AND r.type = br.com.seudominio.foodrescue.domain.enums.RecommendationType.DISCOUNT "
            + "ORDER BY r.respondedAt ASC")
    List<AiRecommendation> findRespondedByEstablishmentAndPeriod(
            @Param("establishmentId") Long establishmentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Finds the latest recommendation of every product that has already been
     * answered or has expired since a given moment, i.e. the tail of each
     * product's chain of the day, ready to be followed by a new one (UC10).
     *
     * @param since the start of the period considered (usually the start of the day)
     * @return the tail recommendations that are no longer pending
     */
    @Query("SELECT r FROM AiRecommendation r WHERE r.active = true AND r.product.active = true "
            + "AND r.status <> br.com.seudominio.foodrescue.domain.enums.RecommendationStatus.PENDING "
            + "AND r.creationDate >= :since "
            + "AND NOT EXISTS (SELECT 1 FROM AiRecommendation n WHERE n.active = true "
            + "AND n.product.id = r.product.id AND n.creationDate > r.creationDate)")
    List<AiRecommendation> findChainTailsSince(@Param("since") LocalDateTime since);

    /**
     * Finds the recommendations of a product made since a given moment,
     * oldest first, so the chain can be read in order (UC10).
     *
     * @param productId the product id
     * @param since     the start of the period considered
     * @return the product's recommendations in chronological order
     */
    List<AiRecommendation> findByProductIdAndCreationDateGreaterThanEqualAndActiveTrueOrderByCreationDateAsc(
            Long productId, LocalDateTime since);
}