package br.com.seudominio.foodrescue.rest;

import br.com.seudominio.foodrescue.domain.FoodItem;
import br.com.seudominio.foodrescue.dtos.FoodItemDTO;
import br.com.seudominio.foodrescue.dtos.RegisterFoodItemDTO;
import br.com.seudominio.foodrescue.mappers.FoodItemMapper;
import br.com.seudominio.perishrescue.business.services.ResourceService;
import br.com.seudominio.perishrescue.rest.controller.AbstractResourceController;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FoodRescue's catalog endpoints for {@link FoodItem}. All the behavior comes
 * from the framework's {@link AbstractResourceController}; this class only
 * binds it to the food item type and to {@code /food-items}.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Tag(name = "Food items", description = "Food item registration, listing and inventory management")
@RestController
@RequestMapping("/food-items")
public class FoodItemController
        extends AbstractResourceController<FoodItem, RegisterFoodItemDTO, FoodItemDTO> {

    /**
     * Constructor.
     *
     * @param resourceService the framework's resource service
     * @param mapper          the food item mapper
     */
    public FoodItemController(ResourceService resourceService, FoodItemMapper mapper) {
        super(resourceService, mapper);
    }
}
