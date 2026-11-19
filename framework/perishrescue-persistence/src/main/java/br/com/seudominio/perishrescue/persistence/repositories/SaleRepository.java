package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.Sale;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sale repository - This class is responsible for the database operations of the sale entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface SaleRepository extends GenericRepository<Sale> {

    /**
     * Finds the active sales of a resource within a period, oldest first.
     *
     * @param resourceId the resource id
     * @param start     the start of the period (inclusive)
     * @param end       the end of the period (exclusive)
     * @return the sales of the resource within the period
     */
    @Query("SELECT s FROM Sale s WHERE s.resource.id = :resourceId AND s.active = true "
            + "AND s.soldAt >= :start AND s.soldAt < :end ORDER BY s.soldAt ASC")
    List<Sale> findByResourceAndPeriod(@Param("resourceId") Long resourceId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    /**
     * Finds the active sales of an offerer within a period, oldest first (UC12).
     *
     * <p>Uses {@code JOIN FETCH} to eagerly load the associated resource and its original price,
     * avoiding N+1 queries when computing indicators.</p>
     *
     * @param offererId the offerer identifier
     * @param start           the start timestamp of the period (inclusive)
     * @param end             the end timestamp of the period (exclusive)
     * @return the sales of the offerer within the period
     */
    @Query("SELECT s FROM Sale s JOIN FETCH s.resource p WHERE p.offerer.id = :offererId "
            + "AND s.active = true AND s.soldAt >= :start AND s.soldAt < :end "
            + "ORDER BY s.soldAt ASC")
    List<Sale> findByOffererAndPeriod(
            @Param("offererId") Long offererId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
