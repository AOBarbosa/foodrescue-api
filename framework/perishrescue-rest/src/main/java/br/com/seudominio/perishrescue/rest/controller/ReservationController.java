package br.com.seudominio.perishrescue.rest.controller;

import br.com.seudominio.perishrescue.business.services.ReservationService;
import br.com.seudominio.perishrescue.domain.dtos.ConfirmPickupRequest;
import br.com.seudominio.perishrescue.domain.dtos.ReservationDTO;
import br.com.seudominio.perishrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;

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
 * Reservation endpoints of the kernel (UC09): beneficiaries reserve, list and
 * cancel; offerers list the reservations on their offers and confirm pickups.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Tag(name = "Reservations", description = "Reservation, pickup and cancellation of offers")
@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    /**
     * Constructor.
     *
     * @param reservationService the reservation service
     */
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * Reserves units of an offer for the authenticated beneficiary.
     *
     * @param request   the offer, quantity and optional attributes for the rescue rules
     * @param principal the authenticated beneficiary
     * @return the reservation, with its pickup code and deadline
     */
    @PreAuthorize("hasRole('ROLE_BENEFICIARY')")
    @Operation(summary = "Reserve units of an available offer")
    @PostMapping
    public ResponseEntity<ApiResponse<ReservationDTO>> reserve(
            @Valid @RequestBody ReserveOfferRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                reservationService.reserve(request, principal.id()),
                "Offer reserved successfully",
                true,
                null));
    }

    /**
     * Lists the authenticated beneficiary's reservations.
     *
     * @param principal the authenticated beneficiary
     * @return the reservations, newest first
     */
    @PreAuthorize("hasRole('ROLE_BENEFICIARY')")
    @Operation(summary = "List the reservations of the authenticated beneficiary")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> list(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                reservationService.findAllForBeneficiary(principal.id()),
                "Reservations retrieved successfully",
                true,
                null));
    }

    /**
     * Gets one of the authenticated beneficiary's reservations.
     *
     * @param id        the reservation id
     * @param principal the authenticated beneficiary
     * @return the reservation
     */
    @PreAuthorize("hasRole('ROLE_BENEFICIARY')")
    @Operation(summary = "Get a reservation of the authenticated beneficiary")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationDTO>> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                reservationService.findByIdForBeneficiary(id, principal.id()),
                "Reservation retrieved successfully",
                true,
                null));
    }

    /**
     * Cancels one of the authenticated beneficiary's reservations; its units go back to the offer.
     *
     * @param id        the reservation id
     * @param principal the authenticated beneficiary
     * @return the cancelled reservation
     */
    @PreAuthorize("hasRole('ROLE_BENEFICIARY')")
    @Operation(summary = "Cancel a reservation of the authenticated beneficiary")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<ReservationDTO>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                reservationService.cancel(id, principal.id()),
                "Reservation cancelled successfully",
                true,
                null));
    }

    /**
     * Lists the reservations made on the authenticated offerer's offers (without pickup codes).
     *
     * @param principal the authenticated offerer
     * @return the reservations, newest first
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "List the reservations made on the authenticated offerer's offers")
    @GetMapping("/received")
    public ResponseEntity<ApiResponse<List<ReservationDTO>>> listReceived(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                reservationService.findAllForOfferer(principal.id()),
                "Reservations retrieved successfully",
                true,
                null));
    }

    /**
     * Confirms, at the counter, that the beneficiary picked the reservation up.
     *
     * @param id        the reservation id
     * @param request   the pickup code the beneficiary showed
     * @param principal the authenticated offerer
     * @return the rescued reservation
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "Confirm the pickup of a reservation with its pickup code")
    @PostMapping("/{id}/pickup")
    public ResponseEntity<ApiResponse<ReservationDTO>> confirmPickup(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmPickupRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                reservationService.confirmPickup(id, principal.id(), request.pickupCode()),
                "Pickup confirmed: reservation rescued",
                true,
                null));
    }
}
