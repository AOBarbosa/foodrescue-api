package br.com.seudominio.foodrescue.rest.security;

/**
 * The two authentication profiles supported by the platform, carried as a
 * claim in the JWT and mapped to a Spring Security authority
 * ({@code ROLE_ESTABLISHMENT}/{@code ROLE_CONSUMER}).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public enum UserRole {

    ESTABLISHMENT,
    CONSUMER
}
