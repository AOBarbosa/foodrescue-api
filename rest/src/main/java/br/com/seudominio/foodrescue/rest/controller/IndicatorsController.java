package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.IndicatorsService;
import br.com.seudominio.foodrescue.domain.dtos.WasteAndSavingsIndicatorsDTO;
import br.com.seudominio.foodrescue.domain.enums.IndicatorPeriod;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Controller for consolidated impact and waste-reduction indicators (UC12).
 *
 * @author Clovis Luan
 * @since 1.0.0
 */
@Tag(name = "Indicators", description = "Consolidated impact and waste-reduction indicators for establishments")
@RestController
@RequestMapping("/indicators")
public class IndicatorsController {

    private final IndicatorsService indicatorsService;

    /**
     * Constructor.
     *
     * @param indicatorsService the indicators service
     */
    public IndicatorsController(IndicatorsService indicatorsService) {
        this.indicatorsService = indicatorsService;
    }

    /**
     * Retrieves the consolidated impact indicators for the authenticated establishment.
     *
     * @param period    the predefined period type (DAY, WEEK, MONTH - defaults to MONTH)
     * @param startDate the custom start date (optional, ISO format YYYY-MM-DD)
     * @param endDate   the custom end date (optional, ISO format YYYY-MM-DD)
     * @param compare   whether to compare with the preceding period of same duration (defaults to false)
     * @param principal the authenticated principal
     * @return the indicators response
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Retrieve consolidated waste and savings indicators for the establishment")
    @GetMapping
    public ResponseEntity<ApiResponse<WasteAndSavingsIndicatorsDTO>> getIndicators(
            @RequestParam(required = false) IndicatorPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean compare,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {

        WasteAndSavingsIndicatorsDTO indicators =
                indicatorsService.getIndicators(principal.id(), period, startDate, endDate, compare);

        return ResponseEntity.ok(new ApiResponse<>(
                indicators,
                "Indicators retrieved successfully",
                true,
                null));
    }
}
