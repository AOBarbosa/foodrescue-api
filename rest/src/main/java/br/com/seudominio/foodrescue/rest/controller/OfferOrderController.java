package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.OfferOrderService;
import br.com.seudominio.foodrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.foodrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for consumer reservations/purchases of surplus offers (UC09).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Tag(name = "Offer Orders", description = "Consumer reservations and purchases of surplus offers")
@RestController
@RequestMapping("/offer-orders")
public class OfferOrderController {

    private final OfferOrderService offerOrderService;

    /**
     * Constructor.
     *
     * @param offerOrderService the offer order service
     */
    public OfferOrderController(OfferOrderService offerOrderService) {
        this.offerOrderService = offerOrderService;
    }

    /**
     * Reserves/purchases units of an active offer for the authenticated consumer.
     *
     * @param request   the reservation request containing the offer id and quantity
     * @param principal the authenticated principal
     * @return the created reservation
     */
    @PreAuthorize("hasRole('ROLE_CONSUMER')")
    @Operation(summary = "Reserve units of an active offer")
    @PostMapping
    public ResponseEntity<ApiResponse<OfferOrderDTO>> reserve(
            @Valid @RequestBody ReserveOfferRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                offerOrderService.reserve(request, principal.id()),
                "Offer reserved successfully",
                true,
                null));
    }

    /**
     * Lists the authenticated consumer's reservations, newest first.
     *
     * @param principal the authenticated principal
     * @return the consumer's reservations
     */
    @PreAuthorize("hasRole('ROLE_CONSUMER')")
    @Operation(summary = "List the reservations of the authenticated consumer")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OfferOrderDTO>>> list(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                offerOrderService.findAllForConsumer(principal.id()),
                "Offer orders retrieved successfully",
                true,
                null));
    }

    /**
     * Fetches one reservation of the authenticated consumer.
     *
     * @param id        the reservation id
     * @param principal the authenticated principal
     * @return the reservation
     */
    @PreAuthorize("hasRole('ROLE_CONSUMER')")
    @Operation(summary = "Get a reservation of the authenticated consumer")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferOrderDTO>> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                offerOrderService.findByIdForConsumer(id, principal.id()),
                "Offer order retrieved successfully",
                true,
                null));
    }
}
