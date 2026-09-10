package br.com.seudominio.foodrescue.domain.repository;

import br.com.seudominio.foodrescue.domain.entities.Product;

import java.util.Optional;

/**
 * Port for persisting and retrieving {@link Product} aggregates.
 * Kept minimal on purpose (ISP) — UC02/UC03 (issues #3/#4) add the finder
 * methods (e.g. by establishment) that their own use cases actually need.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public interface ProductRepository {

    /**
     * Persists a product, either creating or updating it.
     *
     * @param product the product to persist
     * @return the persisted product
     */
    Product save(Product product);

    /**
     * Finds a product by its identifier.
     *
     * @param id the product's identifier
     * @return the product, or empty if not found
     */
    Optional<Product> findById(Long id);
}
