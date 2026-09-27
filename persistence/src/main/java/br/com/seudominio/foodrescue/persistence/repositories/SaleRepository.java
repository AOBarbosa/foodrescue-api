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
     * Finds active sales for a specific product, optionally filtered by a date range.
     * Specific query method tailored for sales history by period (ISP).
     *
     * @param productId the product identifier
     * @param startDate the start timestamp (optional)
     * @param endDate   the end timestamp (optional)
     * @return the list of active sales ordered by soldAt descending
     */
    @Query("""
        SELECT s FROM Sale s
        WHERE s.product.id = :productId
          AND s.active = true
          AND (:startDate IS NULL OR s.soldAt >= :startDate)
          AND (:endDate IS NULL OR s.soldAt <= :endDate)
        ORDER BY s.soldAt DESC
    """)
    List<Sale> findByProductAndPeriod(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
