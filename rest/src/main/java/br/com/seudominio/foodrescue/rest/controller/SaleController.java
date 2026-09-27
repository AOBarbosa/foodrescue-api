package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.SaleService;
import br.com.seudominio.foodrescue.domain.dtos.RegisterSaleDTO;
import br.com.seudominio.foodrescue.domain.dtos.SaleDTO;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for sale recording and history (UC04).
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Tag(name = "Sales", description = "Sale recording and history")
@RestController
@RequestMapping("/sales")
public class SaleController {

    private final SaleService saleService;

    /**
     * Constructor.
     *
     * @param saleService the sale service
     */
    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    /**
     * Registers a new over-the-counter sale for a product owned by the authenticated establishment.
     *
     * @param dto       the sale registration data
     * @param principal the authenticated principal
     * @return the registered sale
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Register a new sale for a product")
    @PostMapping
    public ResponseEntity<ApiResponse<SaleDTO>> create(
            @Valid @RequestBody RegisterSaleDTO dto,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SaleDTO created = saleService.registerSale(dto, principal.id());
        return ResponseEntity.ok(new ApiResponse<>(
                created,
                "Sale registered successfully",
                true,
                null
        ));
    }

    /**
     * Fetches a sale by identifier for the authenticated establishment.
     *
     * @param id        the sale identifier
     * @param principal the authenticated principal
     * @return the sale
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Get a sale by identifier")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SaleDTO>> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        SaleDTO sale = saleService.findByIdForEstablishment(id, principal.id());
        return ResponseEntity.ok(new ApiResponse<>(
                sale,
                "Sale retrieved successfully",
                true,
                null
        ));
    }

    /**
     * Lists sales for a product owned by the authenticated establishment, optionally filtered by period.
     *
     * @param productId the product identifier
     * @param startDate the start timestamp (optional)
     * @param endDate   the end timestamp (optional)
     * @param principal the authenticated principal
     * @return the list of sales
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "List sales of a product optionally filtered by period")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SaleDTO>>> list(
            @RequestParam Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        List<SaleDTO> sales = saleService.findSalesByProductAndPeriod(productId, principal.id(), startDate, endDate);
        return ResponseEntity.ok(new ApiResponse<>(
                sales,
                "Sales retrieved successfully",
                true,
                null
        ));
    }
}
