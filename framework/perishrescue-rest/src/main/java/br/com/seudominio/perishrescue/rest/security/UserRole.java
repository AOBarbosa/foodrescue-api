package br.com.seudominio.perishrescue.rest.security;

/**
 * The two authentication profiles supported by the platform, carried as a
 * claim in the JWT and mapped to a Spring Security authority
 * ({@code ROLE_OFFERER}/{@code ROLE_BENEFICIARY}).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public enum UserRole {

    OFFERER,
    BENEFICIARY
}
