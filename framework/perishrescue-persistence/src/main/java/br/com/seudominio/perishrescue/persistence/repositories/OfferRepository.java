package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Offer repository - database operations of the {@link Offer} entity.
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Repository
public interface OfferRepository extends GenericRepository<Offer> {

    /**
     * Finds the newest offer of a resource in one of the given statuses.
     *
     * @param resourceId the resource identifier
     * @param statuses   the statuses to match (usually the open ones)
     * @return the newest matching offer, if any
     */
    Optional<Offer> findFirstByResourceIdAndStatusInAndActiveTrueOrderByCreationDateDesc(
            Long resourceId, Collection<OfferStatus> statuses);

    /**
     * Finds every offer of a resource in one of the given statuses.
     *
     * @param resourceId the resource identifier
     * @param statuses   the statuses to match
     * @return the matching offers
     */
    List<Offer> findByResourceIdAndStatusInAndActiveTrue(Long resourceId, Collection<OfferStatus> statuses);

    /**
     * Finds every offer of an offerer, newest first.
     *
     * @param offererId the offerer identifier
     * @return the offerer's offers
     */
    List<Offer> findByResourceOffererIdAndActiveTrueOrderByCreationDateDesc(Long offererId);

    /**
     * Finds the offers in one of the given statuses whose deadline is at or before {@code now}.
     *
     * @param statuses the statuses to match (usually the open ones)
     * @param now      the current date and time
     * @return the overdue offers
     */
    List<Offer> findByStatusInAndExpiresAtLessThanEqualAndActiveTrue(Collection<OfferStatus> statuses,
                                                                    LocalDateTime now);

    /**
     * Finds the offers in one of the given statuses whose deadline is after {@code now}.
     *
     * @param statuses the statuses to match (usually the open ones)
     * @param now      the current date and time
     * @return the offers still running
     */
    List<Offer> findByStatusInAndExpiresAtGreaterThanAndActiveTrue(Collection<OfferStatus> statuses,
                                                                  LocalDateTime now);

    /**
     * Finds the offers a beneficiary can reserve: available, not expired, with
     * units left, of an active resource; priority ones first, then the ones
     * closest to their deadline.
     *
     * @param now the current date and time
     * @return the orderable offers
     */
    @Query("SELECT o FROM Offer o JOIN FETCH o.resource p WHERE o.active = true AND p.active = true "
            + "AND o.status = br.com.seudominio.perishrescue.domain.enums.OfferStatus.AVAILABLE "
            + "AND o.expiresAt > :now AND o.availableQuantity > 0 "
            + "ORDER BY o.priority DESC, o.expiresAt ASC")
    List<Offer> findOrderable(@Param("now") LocalDateTime now);
}
