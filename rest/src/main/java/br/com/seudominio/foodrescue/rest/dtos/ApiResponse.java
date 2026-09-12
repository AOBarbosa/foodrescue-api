package br.com.seudominio.foodrescue.rest.dtos;

import br.com.seudominio.foodrescue.business.helpers.MessageCode;

/**
 * Standard response envelope for every REST endpoint, success or failure.
 *
 * @param <T>     the payload type
 * @param data    the payload (an {@link ApiError} when {@code success} is {@code false})
 * @param message a human-readable summary of the outcome
 * @param success whether the request succeeded
 * @param code    the machine-readable code for {@code message}
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ApiResponse<T>(T data, String message, boolean success, MessageCode code) {
}
