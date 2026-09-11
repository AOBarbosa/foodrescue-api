package br.com.seudominio.foodrescue.core.validation;

/**
 * A single validation error, raised by an {@link AbstractValidator}.
 *
 * @param message      the human-readable error message
 * @param field        the invalid field/property path
 * @param invalidValue the value that failed validation
 * @param code         a stable, machine-readable error code
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ValidationError(String message, String field, Object invalidValue, String code) {
}
