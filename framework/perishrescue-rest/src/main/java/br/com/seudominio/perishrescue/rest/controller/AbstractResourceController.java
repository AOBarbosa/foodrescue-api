package br.com.seudominio.perishrescue.rest.controller;

import br.com.seudominio.perishrescue.business.services.ResourceService;
import br.com.seudominio.perishrescue.domain.dtos.ResourceInventoryUpdateDTO;
import br.com.seudominio.perishrescue.domain.dtos.UpdateResourceInventoryRequest;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.mappers.ResourceTypeMapper;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * REST endpoints for an offerer's catalog of perishable resources: register,
 * list, find and update inventory (UC02/UC03).
 *
 * <p><b>Hot spot 1, REST layer (Template Method).</b> The endpoints, the
 * authorization rule (only the owning offerer) and the flow (map the request,
 * let the kernel attach the owner, validate and persist, map the response)
 * are frozen here. The kernel does not know the instance's resource type, so
 * this class is abstract: each instance declares one concrete subclass per
 * resource type, annotated with {@code @RestController} and its own
 * {@code @RequestMapping}, that only supplies its {@link ResourceTypeMapper}:</p>
 *
 * <pre>{@code
 * @RestController
 * @RequestMapping("/food-items")
 * public class FoodItemController
 *         extends AbstractResourceController<FoodItem, RegisterFoodItemDTO, FoodItemDTO> {
 *     public FoodItemController(ResourceService service, FoodItemMapper mapper) {
 *         super(service, mapper);
 *     }
 * }
 * }</pre>
 *
 * @param <R> the concrete resource type
 * @param <Q> the registration request payload
 * @param <D> the response payload
 *
 * @author Clovis Medeiros
 * @author Hugo Jose
 * @since 1.0.0
 */
public abstract class AbstractResourceController<R extends PerishableResource, Q, D> {

    private final ResourceService resourceService;
    private final ResourceTypeMapper<R, Q, D> mapper;

    /**
     * Constructor for subclasses.
     *
     * @param resourceService the kernel's resource service
     * @param mapper          the instance's mapper for this resource type
     */
    protected AbstractResourceController(ResourceService resourceService, ResourceTypeMapper<R, Q, D> mapper) {
        this.resourceService = resourceService;
        this.mapper = mapper;
    }

    /**
     * Registers a new resource for the authenticated offerer.
     *
     * @param request   the registration payload
     * @param principal the authenticated principal
     * @return the registered resource
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "Register a new resource for the authenticated offerer")
    @PostMapping
    public ResponseEntity<ApiResponse<D>> create(@Valid @RequestBody Q request,
                                                 @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        R saved = resourceService.register(mapper.toNewEntity(request), principal.id());
        return ResponseEntity.ok(new ApiResponse<>(
                mapper.toDto(saved),
                "Resource registered successfully",
                true,
                null));
    }

    /**
     * Lists the authenticated offerer's active resources of this type.
     *
     * @param principal the authenticated principal
     * @return the resources
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "List the authenticated offerer's resources")
    @GetMapping
    public ResponseEntity<ApiResponse<List<D>>> list(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        List<D> resources = resourceService.findAllForOfferer(principal.id(), mapper.resourceType())
                .stream()
                .map(mapper::toDto)
                .toList();
        return ResponseEntity.ok(new ApiResponse<>(
                resources,
                "Resources retrieved successfully",
                true,
                null));
    }

    /**
     * Finds one of the authenticated offerer's resources of this type.
     *
     * @param id        the resource id
     * @param principal the authenticated principal
     * @return the resource
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "Find one of the authenticated offerer's resources")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<D>> findById(@PathVariable Long id,
                                                   @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        R resource = resourceService.findResourceOwnedBy(id, principal.id(), mapper.resourceType());
        return ResponseEntity.ok(new ApiResponse<>(
                mapper.toDto(resource),
                "Resource retrieved successfully",
                true,
                null));
    }

    /**
     * Partially updates a resource's stock quantity and/or expiration date.
     *
     * @param id        the resource id
     * @param request   the fields to update
     * @param principal the authenticated principal
     * @return the updated resource, flagging an expiration date in the past
     */
    @PreAuthorize("hasRole('ROLE_OFFERER')")
    @Operation(summary = "Update a resource's stock quantity and expiration date")
    @PatchMapping("/{id}/inventory")
    public ResponseEntity<ApiResponse<ResourceInventoryUpdateDTO>> updateInventory(
            @PathVariable Long id,
            @RequestBody UpdateResourceInventoryRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        resourceService.findResourceOwnedBy(id, principal.id(), mapper.resourceType());
        ResourceInventoryUpdateDTO result = resourceService.updateInventory(id, principal.id(), request);
        String message = result.expirationDateInPast()
                ? "Inventory updated; the expiration date is in the past"
                : "Inventory updated successfully";
        return ResponseEntity.ok(new ApiResponse<>(result, message, true, null));
    }
}
