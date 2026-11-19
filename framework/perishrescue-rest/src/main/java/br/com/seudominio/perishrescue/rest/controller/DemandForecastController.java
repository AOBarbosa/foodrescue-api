package br.com.seudominio.perishrescue.rest.controller;

import br.com.seudominio.perishrescue.business.services.DemandForecastService;
import br.com.seudominio.perishrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for resource demand forecasting (UC05).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Tag(name = "Demand forecast", description = "PerishableResource demand forecasting until the end of the business day")
@RestController
@RequestMapping("/resources/{resourceId}/demand-forecast")
public class DemandForecastController {

    private final DemandForecastService demandForecastService;

    /**
     * Constructor.
     *
     * @param demandForecastService the demand forecast service
     */
    public DemandForecastController(DemandForecastService demandForecastService) {
        this.demandForecastService = demandForecastService;
    }

    /**
     * Calculates and records the demand forecast of an authenticated offerer's resource.
     *
     * @param resourceId the resource id
     * @param principal the authenticated principal
     * @return the recorded forecast
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "Forecast how many units of a resource will be sold until closing time")
    @PostMapping
    public ResponseEntity<ApiResponse<DemandForecastResponse>> predict(
            @PathVariable Long resourceId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                demandForecastService.predictDemand(resourceId, principal.id()),
                "Demand forecast calculated successfully",
                true,
                null));
    }

    /**
     * Fetches the latest demand forecast of an authenticated offerer's resource.
     *
     * @param resourceId the resource id
     * @param principal the authenticated principal
     * @return the latest forecast
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "Get the latest demand forecast of a resource")
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<DemandForecastResponse>> findLatest(
            @PathVariable Long resourceId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                demandForecastService.findLatest(resourceId, principal.id()),
                "Demand forecast retrieved successfully",
                true,
                null));
    }
}
