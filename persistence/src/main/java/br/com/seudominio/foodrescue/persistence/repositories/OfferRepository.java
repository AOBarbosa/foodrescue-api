package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.Offer;

import org.springframework.stereotype.Repository;

/**
 * Offer repository - This class is responsible for the database operations of the offer entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface OfferRepository extends GenericRepository<Offer> {
}
