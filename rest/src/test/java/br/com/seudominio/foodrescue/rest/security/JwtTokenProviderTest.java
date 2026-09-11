package br.com.seudominio.foodrescue.rest.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-at-least-32-bytes-long-for-hs256";

    private JwtTokenProvider provider(long expirationMs) {
        return new JwtTokenProvider(SECRET, expirationMs);
    }

    @Test
    void issuesAndParsesATokenRoundTrip() {
        JwtTokenProvider provider = provider(60_000);

        String token = provider.generateToken(42L, UserRole.CONSUMER);

        assertThat(provider.parseToken(token))
                .isPresent()
                .get()
                .isEqualTo(new AuthenticatedPrincipal(42L, UserRole.CONSUMER));
    }

    @Test
    void rejectsAnExpiredToken() throws InterruptedException {
        JwtTokenProvider provider = provider(1);

        String token = provider.generateToken(42L, UserRole.CONSUMER);
        Thread.sleep(50);

        assertThat(provider.parseToken(token)).isEmpty();
    }

    @Test
    void rejectsATamperedToken() {
        JwtTokenProvider provider = provider(60_000);
        String token = provider.generateToken(42L, UserRole.CONSUMER);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(provider.parseToken(tampered)).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithADifferentSecret() {
        String token = provider(60_000).generateToken(42L, UserRole.CONSUMER);
        JwtTokenProvider otherProvider = new JwtTokenProvider("a-completely-different-secret-32-bytes!!", 60_000);

        assertThat(otherProvider.parseToken(token)).isEmpty();
    }

    @Test
    void rejectsGarbageInput() {
        assertThat(provider(60_000).parseToken("not-a-jwt")).isEmpty();
    }
}
