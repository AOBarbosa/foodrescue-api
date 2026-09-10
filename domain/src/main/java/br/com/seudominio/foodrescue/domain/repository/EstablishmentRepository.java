package br.com.seudominio.foodrescue.domain.repository;

import br.com.seudominio.foodrescue.domain.entities.Establishment;

import java.util.Optional;

/**
 * Port for persisting and retrieving {@link Establishment} aggregates.
 * Kept minimal on purpose (ISP) — UC01 (issue #2) adds the finder methods
 * (e.g. by CNPJ, by email) that its own use cases actually need.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public interface EstablishmentRepository {

    /**
     * Persists an establishment, either creating or updating it.
     *
     * @param establishment the establishment to persist
     * @return the persisted establishment
     */
    Establishment save(Establishment establishment);

    /**
     * Finds an establishment by its identifier.
     *
     * @param id the establishment's identifier
     * @return the establishment, or empty if not found
     */
    Optional<Establishment> findById(Long id);
}
