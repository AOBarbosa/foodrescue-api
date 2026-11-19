package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PerishableResource repository - This class is responsible for the database operations of the resource entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface ResourceRepository extends GenericRepository<PerishableResource> {

    /**
     * Finds all resources by offerer id.
     * @param offererId the offerer id.
     * @return an optional list of resources if found, or empty if not found.
     */
    List<PerishableResource> findAllByOffererIdAndActiveTrue(Long offererId);
}
