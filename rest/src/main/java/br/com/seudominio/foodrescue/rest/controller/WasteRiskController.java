package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.WasteRiskService;
import br.com.seudominio.foodrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for the waste risk of an establishment's products (UC06).
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Tag(name = "Waste risk", description = "Products at risk of being wasted, based on their demand forecast")
@RestController
public class WasteRiskController {

    private final WasteRiskService wasteRiskService;

    /**
     * Constructor.
     *
     * @param wasteRiskService the waste risk service
     */
    public WasteRiskController(WasteRiskService wasteRiskService) {
        this.wasteRiskService = wasteRiskService;
    }

    /**
     * Lists the waste risk of the authenticated establishment's products — the
     * data behind its panel. By default only the flagged ones are returned.
     *
     * @param atRiskOnly whether to keep only the products above the risk threshold
     * @param principal  the authenticated principal
     * @return the assessed products, the riskiest first
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "List the products of the establishment that are at risk of being wasted")
    @GetMapping("/waste-risks")
    public ResponseEntity<ApiResponse<List<WasteRiskDTO>>> list(
            @RequestParam(defaultValue = "true") boolean atRiskOnly,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                wasteRiskService.findAllForEstablishment(principal.id(), atRiskOnly),
                "Waste risks retrieved successfully",
                true,
                null));
    }

    /**
     * Assesses the waste risk of a single product of the authenticated establishment.
     *
     * @param productId the product id
     * @param principal the authenticated principal
     * @return the product's waste risk
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Assess the waste risk of a product from its latest demand forecast")
    @GetMapping("/products/{productId}/waste-risk")
    public ResponseEntity<ApiResponse<WasteRiskDTO>> assess(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                wasteRiskService.assess(productId, principal.id()),
                "Waste risk assessed successfully",
                true,
                null));
    }
}
