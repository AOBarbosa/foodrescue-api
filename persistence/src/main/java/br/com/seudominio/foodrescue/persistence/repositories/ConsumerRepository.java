package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.Consumer;

import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Consumer repository - This class is responsible for the database operations of the consumer entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface ConsumerRepository extends GenericRepository<Consumer> {

    /**
     * Finds a consumer by its login email.
     *
     * @param email the email of the consumer.
     * @return an optional containing the consumer if found, or empty if not found.
     */
    Optional<Consumer> findByEmail(String email);
}
