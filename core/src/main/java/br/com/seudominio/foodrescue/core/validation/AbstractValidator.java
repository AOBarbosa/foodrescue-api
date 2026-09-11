package br.com.seudominio.foodrescue.core.validation;

import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Base implementation of {@link Validator}, accumulating errors found by
 * {@link #doValidate(Object)} and raising a {@link ValidationException}
 * carrying all of them at once.
 *
 * @param <T> the type of entity to validate
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public abstract class AbstractValidator<T> implements Validator<T> {

    protected final List<ValidationError> errors = new ArrayList<>();

    /**
     * Runs the concrete validation rules, accumulating failures via {@link #addError}.
     *
     * @param entity the entity to validate
     */
    protected abstract void doValidate(T entity);

    /**
     * Validates the given entity, raising a {@link ValidationException} if any rule fails.
     *
     * @param entity the entity to validate
     * @throws ValidationException if one or more validation rules fail
     */
    @Override
    public void validate(T entity) {
        errors.clear();
        doValidate(entity);
        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed: " + getMessagesErrors(), errors);
        }
    }

    /**
     * Records a validation error.
     *
     * @param message      the human-readable error message
     * @param field        the invalid field/property path
     * @param invalidValue the value that failed validation
     * @param code         a stable, machine-readable error code
     */
    protected void addError(String message, String field, Object invalidValue, String code) {
        errors.add(new ValidationError(message, field, invalidValue, code));
    }

    /**
     * Concatenates the messages of every accumulated error.
     *
     * @return the concatenated error messages
     */
    protected String getMessagesErrors() {
        return errors.stream().map(ValidationError::message).collect(Collectors.joining("; "));
    }
}
