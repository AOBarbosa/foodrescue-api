package br.com.seudominio.foodrescue.business.ai;

/**
 * Raised when a call to the Gemini API fails or returns an unusable answer.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class GeminiException extends RuntimeException {

    /**
     * Constructs a new exception with the given message.
     *
     * @param message the detail message
     */
    public GeminiException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the given message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying failure
     */
    public GeminiException(String message, Throwable cause) {
        super(message, cause);
    }
}
