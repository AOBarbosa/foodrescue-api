package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.OfferService;
import br.com.seudominio.foodrescue.domain.dtos.CreateOfferRequest;
import br.com.seudominio.foodrescue.domain.dtos.OfferDTO;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for surplus offers (UC08).
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Tag(name = "Offers", description = "Surplus published to consumers, created manually or by an accepted discount")
@RestController
@RequestMapping("/offers")
public class OfferController {

    private final OfferService offerService;

    /**
     * Constructor.
     *
     * @param offerService the offer service
     */
    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * Publishes an offer manually. Only the product is required.
     *
     * @param request   the offer data
     * @param principal the authenticated principal
     * @return the published offer
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Publish an offer of surplus manually")
    @PostMapping
    public ResponseEntity<ApiResponse<OfferDTO>> create(
            @Valid @RequestBody CreateOfferRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                offerService.createOffer(request, principal.id()),
                "Offer published successfully",
                true,
                null));
    }

    /**
     * Lists the authenticated establishment's offers, newest first.
     *
     * @param principal the authenticated principal
     * @return the establishment's offers
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "List the offers of the establishment")
    @GetMapping
    public ResponseEntity<ApiResponse<List<OfferDTO>>> list(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                offerService.findAllForEstablishment(principal.id()),
                "Offers retrieved successfully",
                true,
                null));
    }

    /**
     * Lists the offers a consumer can still order.
     *
     * @return the orderable offers
     */
    @PreAuthorize("hasRole('ROLE_CONSUMER')")
    @Operation(summary = "List the offers available to order")
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<OfferDTO>>> available() {
        return ResponseEntity.ok(new ApiResponse<>(
                offerService.findOrderable(),
                "Available offers retrieved successfully",
                true,
                null));
    }

    /**
     * Fetches one offer of the authenticated establishment.
     *
     * @param id        the offer id
     * @param principal the authenticated principal
     * @return the offer
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Get an offer of the establishment")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OfferDTO>> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                offerService.findByIdForEstablishment(id, principal.id()),
                "Offer retrieved successfully",
                true,
                null));
    }

    /**
     * Closes an offer before its deadline.
     *
     * @param id        the offer id
     * @param principal the authenticated principal
     * @return the closed offer
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Close an active offer")
    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<OfferDTO>> close(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                offerService.closeOffer(id, principal.id()),
                "Offer closed successfully",
                true,
                null));
    }
}
