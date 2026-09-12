package br.com.seudominio.foodrescue.rest.security;

/**
 * The authenticated identity carried by a valid JWT: the id of the
 * establishment/consumer it was issued for, and their role.
 *
 * @param id   the authenticated entity's identifier
 * @param role the authenticated entity's role
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record AuthenticatedPrincipal(Long id, UserRole role) {
}
