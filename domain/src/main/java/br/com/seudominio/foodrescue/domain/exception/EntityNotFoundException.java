package br.com.seudominio.foodrescue.domain.exception;

/**
 * Thrown when a required entity cannot be found by its identifier.
 * Used by use cases that need to fail fast instead of propagating an empty
 * {@code Optional}.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public class EntityNotFoundException extends DomainException {

    /**
     * Constructs a new exception for the given entity type and identifier.
     *
     * @param entityType the type of the entity that was not found
     * @param id         the identifier that was looked up
     */
    public EntityNotFoundException(Class<?> entityType, Object id) {
        super(entityType.getSimpleName() + " not found: " + id);
    }
}
