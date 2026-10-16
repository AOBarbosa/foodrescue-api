package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.enums.OfferStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Offer repository - This class is responsible for the database operations of the offer entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface OfferRepository extends GenericRepository<Offer> {

    /**
     * Finds the most recent offer of a product in a given status, used to stop a
     * second active offer from being published for the same product (UC08).
     *
     * @param productId the product id
     * @param status    the status to filter by
     * @return the most recent matching offer, if any
     */
    Optional<Offer> findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
            Long productId, OfferStatus status);

    /**
     * Finds every offer of a product in a given status (UC08).
     *
     * <p>Used to close whatever is still open when the product runs out of stock.</p>
     *
     * @param productId the product id
     * @param status    the status to filter by
     * @return the matching offers
     */
    List<Offer> findByProductIdAndStatusAndActiveTrue(Long productId, OfferStatus status);

    /**
     * Finds every offer of an establishment, newest first (UC08).
     *
     * @param establishmentId the establishment id
     * @return the establishment's offers
     */
    List<Offer> findByProductEstablishmentIdAndActiveTrueOrderByCreationDateDesc(Long establishmentId);

    /**
     * Finds the offers a consumer can still order: active, not expired and with
     * units left. Highlighted offers come first (UC08, consumed by UC09/UC11).
     *
     * @param now the current moment
     * @return the orderable offers
     */
    @Query("SELECT o FROM Offer o JOIN FETCH o.product p WHERE o.active = true AND p.active = true "
            + "AND o.status = br.com.seudominio.foodrescue.domain.enums.OfferStatus.ACTIVE "
            + "AND o.expiresAt > :now AND o.availableQuantity > 0 "
            + "ORDER BY o.priority DESC, o.expiresAt ASC")
    List<Offer> findOrderable(@Param("now") LocalDateTime now);
}
