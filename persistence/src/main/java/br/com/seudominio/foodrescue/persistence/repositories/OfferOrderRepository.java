package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.OfferOrder;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Offer order repository - This class is responsible for the database operations of the offer order entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface OfferOrderRepository extends GenericRepository<OfferOrder> {

    /**
     * Finds every active reservation made by a consumer, newest first (UC09).
     *
     * @param consumerId the consumer identifier
     * @return the consumer's reservations
     */
    List<OfferOrder> findByConsumerIdAndActiveTrueOrderByCreationDateDesc(Long consumerId);
}
