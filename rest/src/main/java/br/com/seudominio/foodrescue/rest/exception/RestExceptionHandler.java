package br.com.seudominio.foodrescue.rest.exception;

import br.com.seudominio.foodrescue.business.helpers.MessageCode;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.DuplicateEntityException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.rest.dtos.ApiError;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.dtos.ApiSubError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates every exception raised by a controller/use case into the same
 * {@link ApiResponse}/{@link ApiError} JSON shape, so no controller needs a
 * manual {@code try/catch}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@RestControllerAdvice
public class RestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Handles a required entity that couldn't be found.
     *
     * @param ex the exception
     * @return HTTP 404
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFound(EntityNotFoundException ex) {
        return build(new ApiError(HttpStatus.NOT_FOUND, ex.getMessage(), MessageCode.ENTITY_NOT_FOUND));
    }

    /**
     * Handles a uniqueness constraint violation (e.g. a duplicate CNPJ/email).
     *
     * @param ex the exception
     * @return HTTP 409
     */
    @ExceptionHandler(DuplicateEntityException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateEntity(DuplicateEntityException ex) {
        return build(new ApiError(HttpStatus.CONFLICT, ex.getMessage(), MessageCode.DUPLICATE_ENTITY));
    }

    /**
     * Handles a business rule/invariant violation.
     *
     * @param ex the exception
     * @return HTTP 422
     */
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        return build(new ApiError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), MessageCode.BUSINESS_RULE_VIOLATION));
    }

    /**
     * Handles a {@code BusinessValidator} failure, reporting every failed field at once.
     *
     * @param ex      the exception
     * @param request the current request
     * @return HTTP 422 with one sub-error per failed rule
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(ValidationException ex, HttpServletRequest request) {
        ApiError apiError = new ApiError(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), MessageCode.VALIDATION_ERROR);
        apiError.setSubErrors(ex.getErrors().stream()
                .map(error -> ApiSubError.withCode(error.field(), error.message(), error.code()))
                .toList());
        logWarn(ex, request);
        return build(apiError);
    }

    /**
     * Handles a bean-validation failure raised directly (not via {@code @Valid}), e.g. by
     * {@code GenericService}.
     *
     * @param ex      the exception
     * @param request the current request
     * @return HTTP 400 with one sub-error per violated constraint
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Invalid entity data", MessageCode.CONSTRAINT_VIOLATION);
        String objectName = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getRootBeanClass().getSimpleName())
                .orElse("Unknown");
        apiError.setSubErrors(ex.getConstraintViolations().stream()
                .map(violation -> new ApiSubError(
                        objectName,
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue(),
                        violation.getMessage()))
                .toList());
        logWarn(ex, request);
        return build(apiError);
    }

    /**
     * Handles a {@code @Valid @RequestBody} failure. Reports the offending field
     * names and their constraint messages — never the rejected values.
     *
     * @param ex      the exception
     * @param request the current request
     * @return HTTP 400 with one sub-error per invalid field
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, "Invalid request payload", MessageCode.VALIDATION_ERROR);
        apiError.setSubErrors(ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> new ApiSubError(fieldError.getField(), fieldError.getDefaultMessage()))
                .toList());
        logWarn(ex, request);
        return build(apiError);
    }

    /**
     * Handles a request body that couldn't be read or parsed (malformed JSON, an
     * unknown enum value, an empty body). Never echoes {@code ex.getMessage()} —
     * for a malformed body it can quote a fragment of the payload.
     *
     * @param ex      the exception
     * @param request the current request
     * @return HTTP 400 with a generic message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(new ApiError(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body", MessageCode.INVALID_PAYLOAD));
    }

    /**
     * Handles a generic invalid-argument failure (e.g. {@code GenericService}'s
     * not-found fallback).
     *
     * @param ex      the exception
     * @param request the current request
     * @return HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    /**
     * Handles a concurrent-update conflict (e.g. two simultaneous offer reservations).
     *
     * @param ex      the exception
     * @param request the current request
     * @return HTTP 409
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Object>> handleOptimisticLockingFailure(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest request) {
        logWarn(ex, request);
        return build(new ApiError(HttpStatus.CONFLICT, "The resource was modified by another request", MessageCode.OPTIMISTIC_LOCK_CONFLICT));
    }

    /**
     * Handles an authorization failure raised from within a controller (e.g. a
     * {@code @PreAuthorize} check). A missing/invalid JWT is handled earlier, at
     * the security-filter level, by {@code RestAuthenticationEntryPoint}.
     *
     * @param ex the exception
     * @return HTTP 403
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        return build(new ApiError(HttpStatus.FORBIDDEN, "Access denied", MessageCode.ACCESS_DENIED));
    }

    /**
     * Handles an authentication failure raised from within a controller. A
     * missing/invalid JWT is handled earlier, at the security-filter level, by
     * {@code RestAuthenticationEntryPoint}.
     *
     * @param ex the exception
     * @return HTTP 401
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        return build(new ApiError(HttpStatus.UNAUTHORIZED, "Authentication required", MessageCode.UNAUTHORIZED));
    }

    /**
     * Fallback for anything not handled above. Never leaks the exception message
     * or a stacktrace to the client.
     *
     * @param ex      the exception
     * @param request the current request
     * @return HTTP 500 with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", MessageCode.INTERNAL_ERROR));
    }

    private ResponseEntity<ApiResponse<Object>> build(ApiError apiError) {
        return new ResponseEntity<>(
                new ApiResponse<>(apiError, apiError.getMessage(), false, apiError.getMessageCode()), apiError.getStatus());
    }

    private void logWarn(Exception ex, HttpServletRequest request) {
        log.warn("{} {} -> {}: {}", request.getMethod(), request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());
    }
}
