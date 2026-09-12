package br.com.seudominio.foodrescue.core.validation;

/**
 * Base contract for validating an entity, raising a
 * {@link br.com.seudominio.foodrescue.core.validation.exception.ValidationException}
 * when it fails.
 *
 * @param <T> the type of entity to validate
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public interface Validator<T> {

    /**
     * Validates the given entity.
     *
     * @param entity the entity to validate
     */
    void validate(T entity);
}
