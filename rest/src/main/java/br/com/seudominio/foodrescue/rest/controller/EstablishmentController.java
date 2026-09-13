package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.EstablishmentService;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentDTO;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentUpdateDTO;
import br.com.seudominio.foodrescue.domain.dtos.LoginRequest;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.dtos.EstablishmentAuthResponse;
import br.com.seudominio.foodrescue.rest.security.EstablishmentAuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for establishment registration and login (UC01).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Tag(name = "Establishments", description = "Establishment registration and login")
@RestController
@RequestMapping("/establishments")
public class EstablishmentController {

    private final EstablishmentService establishmentService;
    private final EstablishmentAuthenticationService establishmentAuthenticationService;

    /**
     * Constructor.
     *
     * @param establishmentService               the establishment service
     * @param establishmentAuthenticationService the register/login + JWT issuance orchestrator
     */
    public EstablishmentController(
            EstablishmentService establishmentService,
            EstablishmentAuthenticationService establishmentAuthenticationService) {
        this.establishmentService = establishmentService;
        this.establishmentAuthenticationService = establishmentAuthenticationService;
    }

    /**
     * Registers a new establishment and returns a JWT for immediate authentication.
     *
     * @param dto the registration data
     * @return the registered establishment's profile plus a JWT
     */
    @Operation(summary = "Register a new establishment")
    @PostMapping
    public ResponseEntity<ApiResponse<EstablishmentAuthResponse>> register(@Valid @RequestBody EstablishmentDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                establishmentAuthenticationService.register(dto),
                "Establishment registered successfully",
                true,
                null));
    }

    /**
     * Authenticates an establishment and returns a JWT.
     *
     * @param request the login credentials
     * @return the authenticated establishment's profile plus a JWT
     */
    @Operation(summary = "Log in as an establishment")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<EstablishmentAuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(
                establishmentAuthenticationService.login(request),
                "Login successful",
                true,
                null));
    }

    /**
     * Lists every active establishment. Public, so consumers can browse
     * establishments without authenticating.
     *
     * @return the list of active establishments
     */
    @Operation(summary = "List establishments")
    @GetMapping
    public ResponseEntity<ApiResponse<List<EstablishmentDTO>>> findAll() {
        return ResponseEntity.ok(new ApiResponse<>(
                establishmentService.findAll(), "Establishments retrieved successfully", true, null));
    }

    /**
     * Fetches an active establishment's public profile by id.
     *
     * @param id the establishment's id
     * @return the establishment's profile
     */
    @Operation(summary = "Get an establishment by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EstablishmentDTO>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                establishmentService.getById(id), "Establishment retrieved successfully", true, null));
    }

    /**
     * Updates an establishment's own profile. Only the authenticated
     * establishment that owns {@code id} may perform this update.
     *
     * @param id  the establishment's id
     * @param dto the profile data to apply
     * @return the updated establishment's profile
     */
    @Operation(summary = "Update an establishment's own profile")
    @PreAuthorize("hasRole('ESTABLISHMENT') and authentication.principal.id == #id")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EstablishmentDTO>> update(
            @PathVariable Long id, @Valid @RequestBody EstablishmentUpdateDTO dto) {
        EstablishmentDTO establishment = establishmentService.updateProfile(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(establishment, "Establishment updated successfully", true, null));
    }

    /**
     * Deactivates (soft-deletes) an establishment's own account. Only the
     * authenticated establishment that owns {@code id} may perform this.
     *
     * @param id the establishment's id
     * @return no content
     */
    @Operation(summary = "Delete (deactivate) an establishment's own account")
    @PreAuthorize("hasRole('ESTABLISHMENT') and authentication.principal.id == #id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        establishmentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
