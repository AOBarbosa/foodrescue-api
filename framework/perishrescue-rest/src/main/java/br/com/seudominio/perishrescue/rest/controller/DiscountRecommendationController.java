package br.com.seudominio.perishrescue.rest.controller;

import br.com.seudominio.perishrescue.business.services.AiRecommendationService;
import br.com.seudominio.perishrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.perishrescue.domain.dtos.RespondToDiscountRecommendationRequest;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
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
        description = "Suggested discounts for resources at risk of being wasted, and the offerer's answer")
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
     * Generates a discount recommendation for a resource at risk of being wasted.
     *
     * @param resourceId the resource id
     * @param principal the authenticated principal
     * @return the pending recommendation
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "Suggest a discount for a resource flagged as at risk of waste")
    @PostMapping("/resources/{resourceId}/discount-recommendations")
    public ResponseEntity<ApiResponse<DiscountRecommendationDTO>> recommend(
            @PathVariable Long resourceId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                aiRecommendationService.recommendDiscount(resourceId, principal.id()),
                "Discount recommendation generated successfully",
                true,
                null));
    }

    /**
     * Lists every recommendation ever made for a resource, newest first.
     *
     * @param resourceId the resource id
     * @param principal the authenticated principal
     * @return the resource's recommendation history
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "List the recommendation history of a resource")
    @GetMapping("/resources/{resourceId}/discount-recommendations")
    public ResponseEntity<ApiResponse<List<DiscountRecommendationDTO>>> history(
            @PathVariable Long resourceId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                aiRecommendationService.findHistoryForResource(resourceId, principal.id()),
                "Recommendation history retrieved successfully",
                true,
                null));
    }

    /**
     * Lists today's chain of recommendations for a resource, oldest first, from
     * the initial recommendation to each new one that followed it (UC10).
     *
     * @param resourceId the resource id
     * @param principal the authenticated principal
     * @return the resource's chain of recommendations
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "List today's chain of recommendations of a resource")
    @GetMapping("/resources/{resourceId}/recommendation-chain")
    public ResponseEntity<ApiResponse<List<DiscountRecommendationDTO>>> chain(
            @PathVariable Long resourceId,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                aiRecommendationService.findChainForResource(resourceId, principal.id()),
                "Recommendation chain retrieved successfully",
                true,
                null));
    }
    /**
     * Lists the authenticated offerer's recommendations still waiting for
     * an answer, oldest first.
     *
     * @param principal the authenticated principal
     * @return the pending recommendations
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "List the discount recommendations waiting for an answer")
    @GetMapping("/discount-recommendations")
    public ResponseEntity<ApiResponse<List<DiscountRecommendationDTO>>> pending(
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(new ApiResponse<>(
                aiRecommendationService.findPendingForOfferer(principal.id()),
                "Pending recommendations retrieved successfully",
                true,
                null));
    }

    /**
     * Accepts, adjusts or refuses a pending recommendation.
     *
     * @param id        the recommendation id
     * @param request   the offerer's answer
     * @param principal the authenticated principal
     * @return the answered recommendation
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
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
                    case ACCEPTED -> "Recommendation accepted; the resource price was updated";
                    case ADJUSTED -> "Recommendation adjusted; the resource price was updated";
                    default -> "Recommendation refused; the resource price was not changed";
                },
                true,
                null));
    }
}
