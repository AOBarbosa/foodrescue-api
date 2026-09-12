package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.OfferOrder;

import org.springframework.stereotype.Repository;

/**
 * Offer order repository - This class is responsible for the database operations of the offer order entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface OfferOrderRepository extends GenericRepository<OfferOrder> {
}
