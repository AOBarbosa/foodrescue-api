package br.com.seudominio.foodrescue.rest.security;

import br.com.seudominio.foodrescue.business.services.EstablishmentService;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentDTO;
import br.com.seudominio.foodrescue.domain.dtos.LoginRequest;
import br.com.seudominio.foodrescue.rest.dtos.EstablishmentAuthResponse;

import org.springframework.stereotype.Service;

/**
 * Orchestrates establishment registration/login with JWT issuance, so
 * {@code EstablishmentController} only has to translate HTTP requests/responses,
 * not compose the business call with token minting. Lives in {@code rest},
 * not {@code business}, because it depends on {@link JwtTokenProvider} — a
 * rest-layer concern the business layer must not depend on.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
public class EstablishmentAuthenticationService {

    private final EstablishmentService establishmentService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor.
     *
     * @param establishmentService the establishment service
     * @param jwtTokenProvider     the JWT provider
     */
    public EstablishmentAuthenticationService(EstablishmentService establishmentService, JwtTokenProvider jwtTokenProvider) {
        this.establishmentService = establishmentService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new establishment and issues a JWT for it.
     *
     * @param dto the registration data
     * @return the registered establishment's profile plus a JWT
     */
    public EstablishmentAuthResponse register(EstablishmentDTO dto) {
        EstablishmentDTO establishment = establishmentService.save(dto);
        String token = jwtTokenProvider.generateToken(establishment.id(), UserRole.ESTABLISHMENT);
        return new EstablishmentAuthResponse(establishment, token);
    }

    /**
     * Authenticates an establishment and issues a JWT for it.
     *
     * @param request the login credentials
     * @return the authenticated establishment's profile plus a JWT
     */
    public EstablishmentAuthResponse login(LoginRequest request) {
        EstablishmentDTO establishment = establishmentService.login(request);
        String token = jwtTokenProvider.generateToken(establishment.id(), UserRole.ESTABLISHMENT);
        return new EstablishmentAuthResponse(establishment, token);
    }
}
