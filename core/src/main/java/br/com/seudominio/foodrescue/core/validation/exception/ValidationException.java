package br.com.seudominio.foodrescue.core.validation.exception;

import br.com.seudominio.foodrescue.core.validation.ValidationError;

import java.util.List;

/**
 * Thrown by a {@code Validator}/{@code BusinessValidator} when one or more
 * validation errors are found. Carries the structured error list, not just
 * a flat message, so callers (e.g. a REST exception handler) can report
 * every failed field at once instead of one at a time.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class ValidationException extends RuntimeException {

    private final List<ValidationError> errors;

    /**
     * Constructs a new exception with the given message and errors.
     *
     * @param message the detail message
     * @param errors  the validation errors found
     */
    public ValidationException(String message, List<ValidationError> errors) {
        super(message);
        this.errors = errors;
    }

    /**
     * Returns the validation errors found.
     *
     * @return the validation errors
     */
    public List<ValidationError> getErrors() {
        return errors;
    }
}
