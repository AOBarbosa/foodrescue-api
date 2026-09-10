package br.com.seudominio.foodrescue.domain.repository;

import br.com.seudominio.foodrescue.domain.entities.Offer;

import java.util.Optional;

/**
 * Port for persisting and retrieving {@link Offer} aggregates.
 * Kept minimal on purpose (ISP) — UC08/UC09 (issues #10/#11) add the finder
 * methods (e.g. active offers listing) that their own use cases actually need.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public interface OfferRepository {

    /**
     * Persists an offer, either creating or updating it.
     *
     * @param offer the offer to persist
     * @return the persisted offer
     */
    Offer save(Offer offer);

    /**
     * Finds an offer by its identifier.
     *
     * @param id the offer's identifier
     * @return the offer, or empty if not found
     */
    Optional<Offer> findById(Long id);
}
