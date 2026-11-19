package br.com.seudominio.perishrescue.rest.controller;

import br.com.seudominio.perishrescue.business.services.OffererService;
import br.com.seudominio.perishrescue.domain.dtos.OffererDTO;
import br.com.seudominio.perishrescue.domain.dtos.OffererUpdateDTO;
import br.com.seudominio.perishrescue.domain.dtos.LoginRequest;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.dtos.OffererAuthResponse;
import br.com.seudominio.perishrescue.rest.security.OffererAuthenticationService;

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
 * Controller for offerer registration and login (UC01).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Tag(name = "Offerers", description = "Offerer registration and login")
@RestController
@RequestMapping("/offerers")
public class OffererController {

    private final OffererService offererService;
    private final OffererAuthenticationService offererAuthenticationService;

    /**
     * Constructor.
     *
     * @param offererService               the offerer service
     * @param offererAuthenticationService the register/login + JWT issuance orchestrator
     */
    public OffererController(
            OffererService offererService,
            OffererAuthenticationService offererAuthenticationService) {
        this.offererService = offererService;
        this.offererAuthenticationService = offererAuthenticationService;
    }

    /**
     * Registers a new offerer and returns a JWT for immediate authentication.
     *
     * @param dto the registration data
     * @return the registered offerer's profile plus a JWT
     */
    @Operation(summary = "Register a new offerer")
    @PostMapping
    public ResponseEntity<ApiResponse<OffererAuthResponse>> register(@Valid @RequestBody OffererDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                offererAuthenticationService.register(dto),
                "Offerer registered successfully",
                true,
                null));
    }

    /**
     * Authenticates an offerer and returns a JWT.
     *
     * @param request the login credentials
     * @return the authenticated offerer's profile plus a JWT
     */
    @Operation(summary = "Log in as an offerer")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OffererAuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(
                offererAuthenticationService.login(request),
                "Login successful",
                true,
                null));
    }

    /**
     * Lists every active offerer. Public, so beneficiaries can browse
     * offerers without authenticating.
     *
     * @return the list of active offerers
     */
    @Operation(summary = "List offerers")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OffererDTO>>> findAll() {
        return ResponseEntity.ok(new ApiResponse<>(
                offererService.findAll(), "Offerers retrieved successfully", true, null));
    }

    /**
     * Fetches an active offerer's public profile by id.
     *
     * @param id the offerer's id
     * @return the offerer's profile
     */
    @Operation(summary = "Get an offerer by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OffererDTO>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                offererService.getById(id), "Offerer retrieved successfully", true, null));
    }

    /**
     * Updates an offerer's own profile. Only the authenticated
     * offerer that owns {@code id} may perform this update.
     *
     * @param id  the offerer's id
     * @param dto the profile data to apply
     * @return the updated offerer's profile
     */
    @Operation(summary = "Update an offerer's own profile")
    @PreAuthorize("hasRole('OFFERER') and authentication.principal.id == #id")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OffererDTO>> update(
            @PathVariable Long id, @Valid @RequestBody OffererUpdateDTO dto) {
        OffererDTO offerer = offererService.updateProfile(id, dto);
        return ResponseEntity.ok(new ApiResponse<>(offerer, "Offerer updated successfully", true, null));
    }

    /**
     * Deactivates (soft-deletes) an offerer's own account. Only the
     * authenticated offerer that owns {@code id} may perform this.
     *
     * @param id the offerer's id
     * @return no content
     */
    @Operation(summary = "Delete (deactivate) an offerer's own account")
    @PreAuthorize("hasRole('OFFERER') and authentication.principal.id == #id")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        offererService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
