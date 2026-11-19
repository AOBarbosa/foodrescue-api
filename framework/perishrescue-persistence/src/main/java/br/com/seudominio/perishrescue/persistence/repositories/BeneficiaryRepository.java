package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.Beneficiary;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Beneficiary repository - This class is responsible for the database operations of the beneficiary entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface BeneficiaryRepository extends GenericRepository<Beneficiary> {

    /**
     * Finds a beneficiary by its login email.
     *
     * @param email the email of the beneficiary.
     * @return an optional containing the beneficiary if found, or empty if not found.
     */
    Optional<Beneficiary> findByEmail(String email);

    /**
     * Finds a beneficiary by its CPF (digits only).
     *
     * @param document the CPF of the beneficiary.
     * @return an optional containing the beneficiary if found, or empty if not found.
     */
    Optional<Beneficiary> findByDocument(String document);

    /**
     * Loads an active beneficiary and locks its row until the end of the
     * transaction ({@code SELECT ... FOR UPDATE}). The reservation flow uses it
     * so that concurrent reservations by the same beneficiary run one at a
     * time and per-CPF quotas cannot be bypassed.
     *
     * @param id the beneficiary id
     * @return the locked beneficiary, if it exists and is active
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Beneficiary b WHERE b.id = :id AND b.active = true")
    Optional<Beneficiary> findByIdForUpdate(@Param("id") Long id);
}
