package br.com.seudominio.foodrescue.rest.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the {@code Authorization: Bearer <token>} header, validates it via
 * {@link JwtTokenProvider}, and populates the {@link SecurityContextHolder}
 * with an {@link AuthenticatedPrincipal} so downstream authorization checks
 * (route-based or {@code @PreAuthorize}) can rely on it. A missing or
 * invalid token simply leaves the request unauthenticated — enforcement is
 * {@code SecurityConfig}'s job.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructs a new JwtAuthenticationFilter with the given token provider.
     *
     * @param jwtTokenProvider the JWT provider
     */
    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            jwtTokenProvider.parseToken(token).ifPresent(principal -> {
                var authority = new SimpleGrantedAuthority("ROLE_" + principal.role().name());
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }

        filterChain.doFilter(request, response);
    }
}
