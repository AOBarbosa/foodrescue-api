package br.com.seudominio.foodrescue.rest.controller;

import br.com.seudominio.foodrescue.business.services.AiRecommendationService;
import br.com.seudominio.foodrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.foodrescue.domain.dtos.RespondToDiscountRecommendationRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for dynamic price recommendations (UC07).
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Tag(name = "Discount recommendations",
        description = "Suggested discounts for products at risk of being wasted, and the establishment's answer")
@RestController
public class DiscountRecommendationController {

    private final AiRecommendationService aiRecommendationService;

    /**
     * Constructor.
     *
     * @param aiRecommendationService the AI recommendation service
     */
    public DiscountRecommendationController(AiRecommendationService aiRecommendationService) {
        this.aiRecommendationService = aiRecommendationService;
    }

    /**
     * Generates a discount recommendation for a product at risk of being wasted.
     *
     * @param productId the product id
     * @param principal the authenticated principal
     * @return the pending recommendation
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Suggest a discount for a product flagged as at risk of waste")
    @PostMapping("/products/{productId}/discount-recommendations")
    public ResponseEntity<ApiResponse<DiscountRecommendationDTO>> recommend(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                aiRecommendationService.recommendDiscount(productId, principal.id()),
                "Discount recommendation generated successfully",
                true,
                null));
    }

    /**
     * Lists every recommendation ever made for a product, newest first.
     *
     * @param productId the product id
     * @param principal the authenticated principal
     * @return the product's recommendation history
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "List the recommendation history of a product")
    @GetMapping("/products/{productId}/discount-recommendations")
    public ResponseEntity<ApiResponse<List<DiscountRecommendationDTO>>> history(
            @PathVariable Long productId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                aiRecommendationService.findHistoryForProduct(productId, principal.id()),
                "Recommendation history retrieved successfully",
                true,
                null));
    }

    /**
     * Lists the authenticated establishment's recommendations still waiting for
     * an answer, oldest first.
     *
     * @param principal the authenticated principal
     * @return the pending recommendations
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "List the discount recommendations waiting for an answer")
    @GetMapping("/discount-recommendations")
    public ResponseEntity<ApiResponse<List<DiscountRecommendationDTO>>> pending(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                aiRecommendationService.findPendingForEstablishment(principal.id()),
                "Pending recommendations retrieved successfully",
                true,
                null));
    }

    /**
     * Accepts, adjusts or refuses a pending recommendation.
     *
     * @param id        the recommendation id
     * @param request   the establishment's answer
     * @param principal the authenticated principal
     * @return the answered recommendation
     */
    @PreAuthorize("hasRole('ROLE_ESTABLISHMENT')")
    @Operation(summary = "Accept, adjust or refuse a pending discount recommendation")
    @PatchMapping("/discount-recommendations/{id}")
    public ResponseEntity<ApiResponse<DiscountRecommendationDTO>> respond(
            @PathVariable Long id,
            @Valid @RequestBody RespondToDiscountRecommendationRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        DiscountRecommendationDTO answered =
                aiRecommendationService.respondToRecommendation(id, principal.id(), request);

        return ResponseEntity.ok(new ApiResponse<>(
                answered,
                switch (answered.status()) {
                    case ACCEPTED -> "Recommendation accepted; the product price was updated";
                    case ADJUSTED -> "Recommendation adjusted; the product price was updated";
                    default -> "Recommendation refused; the product price was not changed";
                },
                true,
                null));
    }
}
