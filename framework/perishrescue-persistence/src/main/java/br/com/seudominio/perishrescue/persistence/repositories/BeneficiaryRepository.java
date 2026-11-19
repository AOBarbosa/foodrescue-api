package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.Beneficiary;

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
}
