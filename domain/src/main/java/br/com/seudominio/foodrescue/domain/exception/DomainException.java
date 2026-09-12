package br.com.seudominio.foodrescue.domain.exception;

/**
 * Base class for every domain exception. Always unchecked, so use cases and
 * entities don't need to pollute their signatures with {@code throws} clauses.
 * Never thrown directly — only through one of its subclasses.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public abstract class DomainException extends RuntimeException {

    /**
     * Constructs a new domain exception with the given message.
     *
     * @param message the detail message
     */
    protected DomainException(String message) {
        super(message);
    }
}
