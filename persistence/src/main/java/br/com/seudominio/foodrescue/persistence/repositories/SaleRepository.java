package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.Sale;

import org.springframework.stereotype.Repository;

/**
 * Sale repository - This class is responsible for the database operations of the sale entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface SaleRepository extends GenericRepository<Sale> {
}
