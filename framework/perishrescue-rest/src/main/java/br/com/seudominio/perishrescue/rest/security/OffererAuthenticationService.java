package br.com.seudominio.perishrescue.rest.security;

import br.com.seudominio.perishrescue.business.services.OffererService;
import br.com.seudominio.perishrescue.domain.dtos.OffererDTO;
import br.com.seudominio.perishrescue.domain.dtos.LoginRequest;
import br.com.seudominio.perishrescue.rest.dtos.OffererAuthResponse;

import org.springframework.stereotype.Service;

/**
 * Orchestrates offerer registration/login with JWT issuance, so
 * {@code OffererController} only has to translate HTTP requests/responses,
 * not compose the business call with token minting. Lives in {@code rest},
 * not {@code business}, because it depends on {@link JwtTokenProvider} — a
 * rest-layer concern the business layer must not depend on.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
public class OffererAuthenticationService {

    private final OffererService offererService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor.
     *
     * @param offererService the offerer service
     * @param jwtTokenProvider     the JWT provider
     */
    public OffererAuthenticationService(OffererService offererService, JwtTokenProvider jwtTokenProvider) {
        this.offererService = offererService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new offerer and issues a JWT for it.
     *
     * @param dto the registration data
     * @return the registered offerer's profile plus a JWT
     */
    public OffererAuthResponse register(OffererDTO dto) {
        OffererDTO offerer = offererService.save(dto);
        String token = jwtTokenProvider.generateToken(offerer.id(), UserRole.OFFERER);
        return new OffererAuthResponse(offerer, token);
    }

    /**
     * Authenticates an offerer and issues a JWT for it.
     *
     * @param request the login credentials
     * @return the authenticated offerer's profile plus a JWT
     */
    public OffererAuthResponse login(LoginRequest request) {
        OffererDTO offerer = offererService.login(request);
        String token = jwtTokenProvider.generateToken(offerer.id(), UserRole.OFFERER);
        return new OffererAuthResponse(offerer, token);
    }
}
