package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.AiRecommendation;
import br.com.seudominio.perishrescue.domain.enums.RecommendationStatus;

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
     * Finds the whole recommendation history of a resource, newest first (UC07).
     *
     * @param resourceId the resource id
     * @return every active recommendation made for the resource
     */
    List<AiRecommendation> findByResourceIdAndActiveTrueOrderByCreationDateDesc(Long resourceId);

    /**
     * Finds the recommendations of an offerer in a given status, oldest
     * first, so the offerer answers the ones that have waited longest (UC07).
     *
     * @param offererId the offerer id
     * @param status          the status to filter by
     * @return the matching recommendations
     */
    List<AiRecommendation> findByResourceOffererIdAndStatusAndActiveTrueOrderByCreationDateAsc(
            Long offererId, RecommendationStatus status);

    /**
     * Finds the most recent recommendation of a resource in a given status, used
     * to stop a second pending recommendation from being created (UC07).
     *
     * @param resourceId the resource id
     * @param status    the status to filter by
     * @return the most recent matching recommendation, if any
     */
    Optional<AiRecommendation> findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
            Long resourceId, RecommendationStatus status);

    /**
     * Finds the active discount recommendations of an offerer responded within a period,
     * oldest first (UC12).
     *
     * <p>Filters by type {@code DISCOUNT} to avoid mixing with future surplus destination recommendations.</p>
     *
     * @param offererId the offerer identifier
     * @param start           the start timestamp of the period (inclusive)
     * @param end             the end timestamp of the period (exclusive)
     * @return the discount recommendations responded within the period
     */
    @Query("SELECT r FROM AiRecommendation r JOIN r.resource p WHERE p.offerer.id = :offererId "
            + "AND r.active = true AND r.respondedAt >= :start AND r.respondedAt < :end "
            + "AND r.type = br.com.seudominio.perishrescue.domain.enums.RecommendationType.DISCOUNT "
            + "ORDER BY r.respondedAt ASC")
    List<AiRecommendation> findRespondedByOffererAndPeriod(
            @Param("offererId") Long offererId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    /**
     * Finds the latest recommendation of every resource that has already been
     * answered or has expired since a given moment, i.e. the tail of each
     * resource's chain of the day, ready to be followed by a new one (UC10).
     *
     * @param since the start of the period considered (usually the start of the day)
     * @return the tail recommendations that are no longer pending
     */
    @Query("SELECT r FROM AiRecommendation r WHERE r.active = true AND r.resource.active = true "
            + "AND r.status <> br.com.seudominio.perishrescue.domain.enums.RecommendationStatus.PENDING "
            + "AND r.creationDate >= :since "
            + "AND NOT EXISTS (SELECT 1 FROM AiRecommendation n WHERE n.active = true "
            + "AND n.resource.id = r.resource.id AND n.creationDate > r.creationDate)")
    List<AiRecommendation> findChainTailsSince(@Param("since") LocalDateTime since);

    /**
     * Finds the recommendations of a resource made since a given moment,
     * oldest first, so the chain can be read in order (UC10).
     *
     * @param resourceId the resource id
     * @param since     the start of the period considered
     * @return the resource's recommendations in chronological order
     */
    List<AiRecommendation> findByResourceIdAndCreationDateGreaterThanEqualAndActiveTrueOrderByCreationDateAsc(
            Long resourceId, LocalDateTime since);
}