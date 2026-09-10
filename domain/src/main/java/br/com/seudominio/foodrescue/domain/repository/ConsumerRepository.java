package br.com.seudominio.foodrescue.domain.repository;

import br.com.seudominio.foodrescue.domain.entities.Consumer;

import java.util.Optional;

/**
 * Port for persisting and retrieving {@link Consumer} aggregates.
 * {@link #findByEmail(String)} is included because the minimal
 * register/login flow built in this foundation issue needs it to enforce
 * unique emails and to authenticate on login.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public interface ConsumerRepository {

    /**
     * Persists a consumer, either creating or updating it.
     *
     * @param consumer the consumer to persist
     * @return the persisted consumer
     */
    Consumer save(Consumer consumer);

    /**
     * Finds a consumer by its identifier.
     *
     * @param id the consumer's identifier
     * @return the consumer, or empty if not found
     */
    Optional<Consumer> findById(Long id);

    /**
     * Finds a consumer by its login email.
     *
     * @param email the consumer's email
     * @return the consumer, or empty if not found
     */
    Optional<Consumer> findByEmail(String email);
}
