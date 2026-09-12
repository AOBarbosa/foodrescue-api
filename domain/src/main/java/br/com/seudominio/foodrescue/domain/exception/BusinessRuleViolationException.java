package br.com.seudominio.foodrescue.domain.exception;

/**
 * Thrown when a business rule or domain invariant is violated (e.g. a price
 * that must be positive, a quantity that must not be negative). Raised by
 * entities inside their own validation/invariant methods, and by use cases
 * when checking business preconditions.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public class BusinessRuleViolationException extends DomainException {

    /**
     * Constructs a new exception with the given message.
     *
     * @param message the detail message describing the violated rule
     */
    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
