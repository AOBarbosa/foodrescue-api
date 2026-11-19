package br.com.seudominio.perishrescue.rest.controller;

import br.com.seudominio.perishrescue.business.services.BeneficiaryService;
import br.com.seudominio.perishrescue.domain.dtos.BeneficiaryDTO;
import br.com.seudominio.perishrescue.domain.dtos.LoginRequest;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.dtos.BeneficiaryAuthResponse;
import br.com.seudominio.perishrescue.rest.security.JwtTokenProvider;
import br.com.seudominio.perishrescue.rest.security.UserRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for beneficiary registration and login. There is no dedicated UC
 * for beneficiary registration — it's covered by the architectural foundation
 * (issue #1), since UC09 requires a registered/authenticated beneficiary as a
 * precondition.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Tag(name = "Beneficiaries", description = "Beneficiary registration and login")
@RestController
@RequestMapping("/beneficiaries")
public class BeneficiaryController {

    private final BeneficiaryService beneficiaryService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor.
     *
     * @param beneficiaryService  the beneficiary service
     * @param jwtTokenProvider the JWT provider
     */
    public BeneficiaryController(BeneficiaryService beneficiaryService, JwtTokenProvider jwtTokenProvider) {
        this.beneficiaryService = beneficiaryService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new beneficiary and returns a JWT for immediate authentication.
     *
     * @param dto the registration data
     * @return the registered beneficiary's profile plus a JWT
     */
    @Operation(summary = "Register a new beneficiary")
    @PostMapping
    public ResponseEntity<ApiResponse<BeneficiaryAuthResponse>> register(@Valid @RequestBody BeneficiaryDTO dto) {
        BeneficiaryDTO beneficiary = beneficiaryService.save(dto);
        String token = jwtTokenProvider.generateToken(beneficiary.id(), UserRole.BENEFICIARY);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                new BeneficiaryAuthResponse(beneficiary, token),
                "Beneficiary registered successfully",
                true,
                null));
    }

    /**
     * Authenticates a beneficiary and returns a JWT.
     *
     * @param request the login credentials
     * @return the authenticated beneficiary's profile plus a JWT
     */
    @Operation(summary = "Log in as a beneficiary")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<BeneficiaryAuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        BeneficiaryDTO beneficiary = beneficiaryService.login(request);
        String token = jwtTokenProvider.generateToken(beneficiary.id(), UserRole.BENEFICIARY);
        return ResponseEntity.ok(new ApiResponse<>(
                new BeneficiaryAuthResponse(beneficiary, token),
                "Login successful",
                true,
                null));
    }
}
