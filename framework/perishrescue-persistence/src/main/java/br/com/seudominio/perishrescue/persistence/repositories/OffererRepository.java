package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.Offerer;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Offerer repository - This class is responsible for the database operations of the offerer entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface OffererRepository extends GenericRepository<Offerer> {

    /**
     * Finds an offerer by its login email.
     *
     * @param email the email of the offerer.
     * @return an optional containing the offerer if found, or empty if not found.
     */
    Optional<Offerer> findByEmail(String email);

    /**
     * Finds an offerer by its CNPJ.
     *
     * @param cnpj the CNPJ of the offerer.
     * @return an optional containing the offerer if found, or empty if not found.
     */
    Optional<Offerer> findByCnpj(String cnpj);
}
