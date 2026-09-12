package br.com.seudominio.foodrescue.rest.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Issues and validates the HS256 JWTs used to authenticate establishments and
 * consumers. The token carries the authenticated entity's id (subject) and
 * role (claim), matching {@link AuthenticatedPrincipal}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey key;
    private final long expirationMs;

    /**
     * Constructs a new JwtTokenProvider with the configured secret and expiration.
     *
     * @param secret       the HMAC signing secret (at least 32 bytes for HS256)
     * @param expirationMs how long, in milliseconds, an issued token stays valid
     */
    public JwtTokenProvider(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Issues a new token for the given identity.
     *
     * @param id   the authenticated entity's identifier
     * @param role the authenticated entity's role
     * @return the signed JWT
     */
    public String generateToken(Long id, UserRole role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim(ROLE_CLAIM, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    /**
     * Validates a token and extracts the identity it was issued for.
     *
     * @param token the JWT to validate
     * @return the authenticated identity, or empty if the token is missing, expired, malformed or tampered with
     */
    public Optional<AuthenticatedPrincipal> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long id = Long.valueOf(claims.getSubject());
            UserRole role = UserRole.valueOf(claims.get(ROLE_CLAIM, String.class));
            return Optional.of(new AuthenticatedPrincipal(id, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
