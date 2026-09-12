package br.com.seudominio.foodrescue.rest.dtos;

import br.com.seudominio.foodrescue.domain.dtos.ConsumerDTO;

/**
 * Response for a successful consumer registration or login: the consumer's
 * profile plus the JWT to use on subsequent requests.
 *
 * @param consumer the authenticated consumer's profile
 * @param token    the JWT bearer token
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ConsumerAuthResponse(ConsumerDTO consumer, String token) {
}
