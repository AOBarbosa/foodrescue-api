package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.ConsumerService;
import br.com.seudominio.foodrescue.domain.dtos.ConsumerDTO;
import br.com.seudominio.foodrescue.domain.dtos.LoginRequest;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.dtos.ConsumerAuthResponse;
import br.com.seudominio.foodrescue.rest.security.JwtTokenProvider;
import br.com.seudominio.foodrescue.rest.security.UserRole;

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
 * Controller for consumer registration and login. There is no dedicated UC
 * for consumer registration — it's covered by the architectural foundation
 * (issue #1), since UC09 requires a registered/authenticated consumer as a
 * precondition.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Tag(name = "Consumers", description = "Consumer registration and login")
@RestController
@RequestMapping("/consumers")
public class ConsumerController {

    private final ConsumerService consumerService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor.
     *
     * @param consumerService  the consumer service
     * @param jwtTokenProvider the JWT provider
     */
    public ConsumerController(ConsumerService consumerService, JwtTokenProvider jwtTokenProvider) {
        this.consumerService = consumerService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Registers a new consumer and returns a JWT for immediate authentication.
     *
     * @param dto the registration data
     * @return the registered consumer's profile plus a JWT
     */
    @Operation(summary = "Register a new consumer")
    @PostMapping
    public ResponseEntity<ApiResponse<ConsumerAuthResponse>> register(@Valid @RequestBody ConsumerDTO dto) {
        ConsumerDTO consumer = consumerService.save(dto);
        String token = jwtTokenProvider.generateToken(consumer.id(), UserRole.CONSUMER);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                new ConsumerAuthResponse(consumer, token),
                "Consumer registered successfully",
                true,
                null));
    }

    /**
     * Authenticates a consumer and returns a JWT.
     *
     * @param request the login credentials
     * @return the authenticated consumer's profile plus a JWT
     */
    @Operation(summary = "Log in as a consumer")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<ConsumerAuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        ConsumerDTO consumer = consumerService.login(request);
        String token = jwtTokenProvider.generateToken(consumer.id(), UserRole.CONSUMER);
        return ResponseEntity.ok(new ApiResponse<>(
                new ConsumerAuthResponse(consumer, token),
                "Login successful",
                true,
                null));
    }
}
