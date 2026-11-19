package br.com.seudominio.perishrescue.rest.controller;

import br.com.seudominio.perishrescue.business.services.WasteRiskService;
import br.com.seudominio.perishrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
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
 * Controller for the waste risk of an offerer's resources (UC06).
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Tag(name = "Waste risk", description = "Resources at risk of being wasted, based on their demand forecast")
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
     * Lists the waste risk of the authenticated offerer's resources — the
     * data behind its panel. By default only the flagged ones are returned.
     *
     * @param atRiskOnly whether to keep only the resources above the risk threshold
     * @param principal  the authenticated principal
     * @return the assessed resources, the riskiest first
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "List the resources of the offerer that are at risk of being wasted")
    @GetMapping("/waste-risks")
    public ResponseEntity<ApiResponse<List<WasteRiskDTO>>> list(
            @RequestParam(defaultValue = "true") boolean atRiskOnly,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                wasteRiskService.findAllForOfferer(principal.id(), atRiskOnly),
                "Waste risks retrieved successfully",
                true,
                null));
    }

    /**
     * Assesses the waste risk of a single resource of the authenticated offerer.
     *
     * @param resourceId the resource id
     * @param principal the authenticated principal
     * @return the resource's waste risk
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "Assess the waste risk of a resource from its latest demand forecast")
    @GetMapping("/resources/{resourceId}/waste-risk")
    public ResponseEntity<ApiResponse<WasteRiskDTO>> assess(
            @PathVariable Long resourceId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                wasteRiskService.assess(resourceId, principal.id()),
                "Waste risk assessed successfully",
                true,
                null));
    }
}
