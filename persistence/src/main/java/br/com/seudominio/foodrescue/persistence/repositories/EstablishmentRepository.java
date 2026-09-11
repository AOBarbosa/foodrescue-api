package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.Establishment;

import org.springframework.stereotype.Repository;

/**
 * Establishment repository - This class is responsible for the database operations of the establishment entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface EstablishmentRepository extends GenericRepository<Establishment> {
}
