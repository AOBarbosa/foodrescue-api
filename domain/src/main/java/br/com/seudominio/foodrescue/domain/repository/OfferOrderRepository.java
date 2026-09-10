package br.com.seudominio.foodrescue.domain.repository;

import br.com.seudominio.foodrescue.domain.entities.OfferOrder;

import java.util.Optional;

/**
 * Port for persisting and retrieving {@link OfferOrder} aggregates.
 * Kept minimal on purpose (ISP) — UC09 (issue #11) adds the finder methods
 * that its own use cases actually need.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public interface OfferOrderRepository {

    /**
     * Persists an offer order, either creating or updating it.
     *
     * @param offerOrder the offer order to persist
     * @return the persisted offer order
     */
    OfferOrder save(OfferOrder offerOrder);

    /**
     * Finds an offer order by its identifier.
     *
     * @param id the offer order's identifier
     * @return the offer order, or empty if not found
     */
    Optional<OfferOrder> findById(Long id);
}
