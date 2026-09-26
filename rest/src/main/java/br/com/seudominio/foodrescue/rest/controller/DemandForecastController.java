package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.DemandForecastService;
import br.com.seudominio.foodrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
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
 * Controller for product demand forecasting (UC05).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Tag(name = "Demand forecast", description = "Product demand forecasting until the end of the business day")
@RestController
@RequestMapping("/products/{productId}/demand-forecast")
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
     * Calculates and records the demand forecast of an authenticated establishment's product.
     *
     * @param productId the product id
     * @param principal the authenticated principal
     * @return the recorded forecast
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Forecast how many units of a product will be sold until closing time")
    @PostMapping
    public ResponseEntity<ApiResponse<DemandForecastResponse>> predict(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                demandForecastService.predictDemand(productId, principal.id()),
                "Demand forecast calculated successfully",
                true,
                null));
    }

    /**
     * Fetches the latest demand forecast of an authenticated establishment's product.
     *
     * @param productId the product id
     * @param principal the authenticated principal
     * @return the latest forecast
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Get the latest demand forecast of a product")
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<DemandForecastResponse>> findLatest(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                demandForecastService.findLatest(productId, principal.id()),
                "Demand forecast retrieved successfully",
                true,
                null));
    }
}
