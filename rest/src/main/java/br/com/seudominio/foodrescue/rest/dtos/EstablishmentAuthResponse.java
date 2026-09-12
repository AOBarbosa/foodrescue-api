package br.com.seudominio.foodrescue.rest.dtos;

import br.com.seudominio.foodrescue.domain.dtos.EstablishmentDTO;

/**
 * Response for a successful establishment registration or login: the
 * establishment's profile plus the JWT to use on subsequent requests.
 *
 * @param establishment the authenticated establishment's profile
 * @param token         the JWT bearer token
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record EstablishmentAuthResponse(EstablishmentDTO establishment, String token) {
}
