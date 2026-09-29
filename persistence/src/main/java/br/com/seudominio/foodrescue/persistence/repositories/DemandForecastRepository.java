package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.DemandForecast;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    /**
     * Finds the most recently calculated active forecast of every product of an
     * establishment, so the waste-risk panel (UC06) can be built with a single
     * query instead of one per product.
     *
     * @param establishmentId the establishment id
     * @return the latest forecast of each forecast product, product name ascending
     */
    @Query("SELECT f FROM DemandForecast f JOIN f.product p WHERE p.establishment.id = :establishmentId "
            + "AND f.active = true AND p.active = true "
            + "AND f.calculatedAt = (SELECT MAX(l.calculatedAt) FROM DemandForecast l "
            + "WHERE l.product.id = p.id AND l.active = true) ORDER BY p.name ASC")
    List<DemandForecast> findLatestByEstablishment(@Param("establishmentId") Long establishmentId);
}
