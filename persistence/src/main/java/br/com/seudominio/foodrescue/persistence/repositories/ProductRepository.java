package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.Product;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Product repository - This class is responsible for the database operations of the product entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface ProductRepository extends GenericRepository<Product> {

    /**
     * Finds all products by establishment id.
     * @param establishmentId the establishment id.
     * @return an optional list of products if found, or empty if not found.
     */
    List<Product> findAllByEstablishmentId(Long establishmentId);
}
