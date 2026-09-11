package br.com.seudominio.foodrescue.domain.dtos;

/**
 * Login credentials, shared by every actor that authenticates against the platform.
 *
 * @param email    the login email
 * @param password the raw password
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record LoginRequest(String email, String password) {
}
