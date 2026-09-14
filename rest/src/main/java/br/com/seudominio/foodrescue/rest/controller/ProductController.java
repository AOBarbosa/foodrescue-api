package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.ProductService;
import br.com.seudominio.foodrescue.domain.dtos.ProductDTO;
import br.com.seudominio.foodrescue.domain.dtos.ProductInventoryUpdateDTO;
import br.com.seudominio.foodrescue.domain.dtos.RegisterProductDTO;
import br.com.seudominio.foodrescue.domain.dtos.UpdateProductInventoryRequest;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for product registration, listing and inventory management (UC02/UC03).
 *
 * @author Clovis Medeiros
 * @author Hugo Jose
 * @since 1.0.0
 */
@Tag(name = "Products", description = "Product registration, listing and inventory management")
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    /**
     * Constructor.
     *
     * @param productService the product service
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Registers a new product.
     *
     * @param dto the product registration data
     * @param principal the authenticated principal
     * @return the registered product
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @PostMapping
    public ResponseEntity<ApiResponse<ProductDTO>> create(@Valid @RequestBody RegisterProductDTO dto,
                                                          @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                productService.registerProduct(dto, principal.id()),
                "Product registered successfully",
                true,
                null));
    }

    /**
     * Lists all products registered by the authenticated establishment.
     *
     * @param principal the authenticated principal
     * @return the list of authenticated establishment's products
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> list(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                productService.findAllForEstablishment(principal.id()),
                "Products retrieved successfully",
                true,
                null));
    }

    /**
     * Updates the inventory fields of an authenticated establishment's product.
     *
     * @param id the product id
     * @param request the inventory fields to update
     * @param principal the authenticated principal
     * @return the updated product and its expiration warning
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Update a product's stock quantity and expiration date")
    @PatchMapping("/{id}/inventory")
    public ResponseEntity<ApiResponse<ProductInventoryUpdateDTO>> updateInventory(
            @PathVariable Long id,
            @RequestBody UpdateProductInventoryRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        ProductInventoryUpdateDTO result = productService.updateInventory(id, principal.id(), request);
        String message = result.expirationDateInPast()
                ? "Inventory updated; the expiration date is in the past"
                : "Inventory updated successfully";

        return ResponseEntity.ok(new ApiResponse<>(result, message, true, null));
    }

    /**
     * Fetches an authenticated establishment's product by id.
     *
     * @param id the product's id
     * @param principal the authenticated principal
     * @return the product
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> findById(@PathVariable Long id,
                                                            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                productService.findByIdForEstablishment(id, principal.id()),
                "Product retrieved successfully",
                true,
                null));
    }




}
