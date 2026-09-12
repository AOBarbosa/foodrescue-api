package br.com.seudominio.foodrescue.rest.dtos;

import br.com.seudominio.foodrescue.business.helpers.MessageCode;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * The error payload carried in an {@link ApiResponse} when a request fails.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class ApiError {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final HttpStatus status;
    private String message;
    private MessageCode messageCode;
    private List<ApiSubError> subErrors;

    /**
     * Constructs an error with only a status.
     *
     * @param status the HTTP status
     */
    public ApiError(HttpStatus status) {
        this.status = status;
    }

    /**
     * Constructs an error with a status and message.
     *
     * @param status  the HTTP status
     * @param message the error message
     */
    public ApiError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     * Constructs an error with a status, message and machine-readable code.
     *
     * @param status      the HTTP status
     * @param message     the error message
     * @param messageCode the machine-readable error code
     */
    public ApiError(HttpStatus status, String message, MessageCode messageCode) {
        this.status = status;
        this.message = message;
        this.messageCode = messageCode;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MessageCode getMessageCode() {
        return messageCode;
    }

    public void setMessageCode(MessageCode messageCode) {
        this.messageCode = messageCode;
    }

    public List<ApiSubError> getSubErrors() {
        return subErrors;
    }

    public void setSubErrors(List<ApiSubError> subErrors) {
        this.subErrors = subErrors;
    }
}
