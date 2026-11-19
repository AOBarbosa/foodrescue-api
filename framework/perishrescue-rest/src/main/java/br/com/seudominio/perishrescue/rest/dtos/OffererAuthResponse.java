package br.com.seudominio.perishrescue.rest.dtos;

import br.com.seudominio.perishrescue.domain.dtos.OffererDTO;

/**
 * Response for a successful offerer registration or login: the
 * offerer's profile plus the JWT to use on subsequent requests.
 *
 * @param offerer the authenticated offerer's profile
 * @param token         the JWT bearer token
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record OffererAuthResponse(OffererDTO offerer, String token) {
}
