package br.com.seudominio.foodrescue.business.validation;

import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.Validator;
import br.com.seudominio.foodrescue.domain.entities.AbstractEntity;

/**
 * Base interface for business validators.
 * Extends validation to include business rules scoped to specific operations.
 *
 * @param <T> the type of entity to validate
 * @author Andre Barbosa
 * @since 1.0.0
 */
public interface BusinessValidator<T extends AbstractEntity> extends Validator<T> {

    /**
     * Validates an entity in the context of a business operation.
     *
     * @param entity    the entity to validate
     * @param operation the operation being performed
     */
    void validateOperation(T entity, BusinessOperation operation);
}
