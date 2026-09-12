package br.com.seudominio.foodrescue.rest.dtos;

/**
 * One field-level error nested inside an {@link ApiError}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class ApiSubError {

    private String object;
    private String field;
    private Object rejectedValue;
    private String message;
    private String code;

    /**
     * Constructs a sub-error for a field-level failure with no object context
     * (e.g. a {@code @Valid} request body field).
     *
     * @param field   the invalid field
     * @param message the error message
     */
    public ApiSubError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    /**
     * Constructs a sub-error for a bean-validation constraint violation.
     *
     * @param object        the simple name of the validated type
     * @param field         the invalid field/property path
     * @param rejectedValue the value that failed validation
     * @param message       the error message
     */
    public ApiSubError(String object, String field, Object rejectedValue, String message) {
        this.object = object;
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.message = message;
    }

    /**
     * Constructs a sub-error carrying a stable, machine-readable code (as produced
     * by {@code core.validation}'s {@code ValidationError}).
     *
     * @param field   the invalid field
     * @param message the error message
     * @param code    the machine-readable error code
     * @return the constructed sub-error
     */
    public static ApiSubError withCode(String field, String message, String code) {
        ApiSubError subError = new ApiSubError(field, message);
        subError.code = code;
        return subError;
    }

    public String getObject() {
        return object;
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }
}
