package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.DemandForecast;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DemandForecast repository - This class is responsible for the database operations of the demand forecast entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface DemandForecastRepository extends GenericRepository<DemandForecast> {

    /**
     * Finds the most recently calculated active forecast of a product.
     *
     * @param productId the product id
     * @return the latest forecast, or empty if the product was never forecast
     */
    Optional<DemandForecast> findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(Long productId);
}
