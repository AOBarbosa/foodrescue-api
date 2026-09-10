package br.com.seudominio.foodrescue.domain.exception;

/**
 * Thrown when a uniqueness constraint is violated (e.g. a CNPJ or email that
 * is already registered). Distinct from {@link BusinessRuleViolationException}
 * because it maps to a different HTTP status ({@code 409 Conflict}) and has a
 * different meaning: a conflict with existing data, not an invalid value.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public class DuplicateEntityException extends DomainException {

    /**
     * Constructs a new exception with the given message.
     *
     * @param message the detail message describing the duplicate constraint
     */
    public DuplicateEntityException(String message) {
        super(message);
    }
}
