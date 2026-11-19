package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.OfferOrder;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Offer order repository - This class is responsible for the database operations of the offer order entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface OfferOrderRepository extends GenericRepository<OfferOrder> {

    /**
     * Finds every active reservation made by a beneficiary, newest first (UC09).
     *
     * @param beneficiaryId the beneficiary identifier
     * @return the beneficiary's reservations
     */
    List<OfferOrder> findByBeneficiaryIdAndActiveTrueOrderByCreationDateDesc(Long beneficiaryId);
}
