package br.com.seudominio.perishrescue.rest.dtos;

import br.com.seudominio.perishrescue.domain.dtos.BeneficiaryDTO;

/**
 * Response for a successful beneficiary registration or login: the beneficiary's
 * profile plus the JWT to use on subsequent requests.
 *
 * @param beneficiary the authenticated beneficiary's profile
 * @param token    the JWT bearer token
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record BeneficiaryAuthResponse(BeneficiaryDTO beneficiary, String token) {
}
