package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.Sale;

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
     * Finds the active sales of a product within a period, oldest first.
     *
     * @param productId the product id
     * @param start     the start of the period (inclusive)
     * @param end       the end of the period (exclusive)
     * @return the sales of the product within the period
     */
    @Query("SELECT s FROM Sale s WHERE s.product.id = :productId AND s.active = true "
            + "AND s.soldAt >= :start AND s.soldAt < :end ORDER BY s.soldAt ASC")
    List<Sale> findByProductAndPeriod(@Param("productId") Long productId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
}
