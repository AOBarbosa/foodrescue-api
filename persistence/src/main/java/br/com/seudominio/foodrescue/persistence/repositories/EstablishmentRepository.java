package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.Establishment;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Establishment repository - This class is responsible for the database operations of the establishment entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface EstablishmentRepository extends GenericRepository<Establishment> {

    /**
     * Finds an establishment by its login email.
     *
     * @param email the email of the establishment.
     * @return an optional containing the establishment if found, or empty if not found.
     */
    Optional<Establishment> findByEmail(String email);

    /**
     * Finds an establishment by its CNPJ.
     *
     * @param cnpj the CNPJ of the establishment.
     * @return an optional containing the establishment if found, or empty if not found.
     */
    Optional<Establishment> findByCnpj(String cnpj);
}
