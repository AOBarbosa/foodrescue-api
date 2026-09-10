package br.com.seudominio.foodrescue.domain.repository;

import br.com.seudominio.foodrescue.domain.entities.Sale;

import java.util.Optional;

/**
 * Port for persisting and retrieving {@link Sale} aggregates.
 * Kept minimal on purpose (ISP) — UC04/UC05 (issues #5/#6) add the finder
 * methods (e.g. by product and period) that their own use cases actually need.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public interface SaleRepository {

    /**
     * Persists a sale, either creating or updating it.
     *
     * @param sale the sale to persist
     * @return the persisted sale
     */
    Sale save(Sale sale);

    /**
     * Finds a sale by its identifier.
     *
     * @param id the sale's identifier
     * @return the sale, or empty if not found
     */
    Optional<Sale> findById(Long id);
}
