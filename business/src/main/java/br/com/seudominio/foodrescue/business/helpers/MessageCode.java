package br.com.seudominio.foodrescue.business.helpers;

/**
 * Stable, machine-readable codes identifying the kind of error behind an
 * {@code ApiError}, each paired with a human-readable description.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public enum MessageCode {

    VALIDATION_ERROR("Request payload failed validation"),
    INVALID_PAYLOAD("Malformed or unreadable request body"),
    CONSTRAINT_VIOLATION("Entity violates one or more validation constraints"),
    ENTITY_NOT_FOUND("Entity not found"),
    DUPLICATE_ENTITY("Entity already exists"),
    BUSINESS_RULE_VIOLATION("Business rule violation"),
    OPTIMISTIC_LOCK_CONFLICT("The resource was modified by another request"),
    ACCESS_DENIED("Access denied"),
    UNAUTHORIZED("Authentication required"),
    INTERNAL_ERROR("An unexpected error occurred");

    private final String description;

    MessageCode(String description) {
        this.description = description;
    }

    /**
     * Returns the human-readable description of this code.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
