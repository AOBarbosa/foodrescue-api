package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.DemandForecast;

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
     * Finds the most recently calculated active forecast of a resource.
     *
     * @param resourceId the resource id
     * @return the latest forecast, or empty if the resource was never forecast
     */
    Optional<DemandForecast> findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(Long resourceId);

    /**
     * Finds the most recently calculated active forecast of every resource of an
     * offerer, so the waste-risk panel (UC06) can be built with a single
     * query instead of one per resource.
     *
     * @param offererId the offerer id
     * @return the latest forecast of each forecast resource, resource name ascending
     */
    @Query("SELECT f FROM DemandForecast f JOIN f.resource p WHERE p.offerer.id = :offererId "
            + "AND f.active = true AND p.active = true "
            + "AND f.calculatedAt = (SELECT MAX(l.calculatedAt) FROM DemandForecast l "
            + "WHERE l.resource.id = p.id AND l.active = true) ORDER BY p.name ASC")
    List<DemandForecast> findLatestByOfferer(@Param("offererId") Long offererId);
}
