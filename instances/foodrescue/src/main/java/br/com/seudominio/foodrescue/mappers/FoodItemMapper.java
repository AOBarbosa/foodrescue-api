package br.com.seudominio.foodrescue.mappers;

import br.com.seudominio.foodrescue.domain.FoodItem;
import br.com.seudominio.foodrescue.dtos.FoodItemDTO;
import br.com.seudominio.foodrescue.dtos.RegisterFoodItemDTO;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.mappers.ResourceTypeMapper;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * FoodRescue's {@link ResourceTypeMapper} for {@link FoodItem}.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class FoodItemMapper implements ResourceTypeMapper<FoodItem, RegisterFoodItemDTO, FoodItemDTO> {

    @Override
    public Class<FoodItem> resourceType() {
        return FoodItem.class;
    }

    @Override
    public FoodItem toNewEntity(RegisterFoodItemDTO request) {
        if (request == null) {
            return null;
        }
        FoodItem item = FoodItem.builder()
                .name(request.name())
                .category(request.category())
                .originalPrice(Money.of(request.originalPrice()))
                .photoUrl(request.photoUrl())
                .build();
        item.setAllergens(request.allergens());
        item.setStorageTemperature(request.storageTemperature());
        item.setSurpriseBag(request.surpriseBag());
        return item;
    }

    @Override
    public FoodItemDTO toDto(FoodItem item) {
        if (item == null) {
            return null;
        }
        return new FoodItemDTO(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getOriginalPrice().amount(),
                item.getCurrentPrice().amount(),
                item.getPhotoUrl(),
                item.getStockQuantity(),
                item.getExpirationDate(),
                item.getOfferer().getId(),
                item.getModificationDate(),
                Set.copyOf(item.getAllergens()),
                item.getStorageTemperature(),
                item.isSurpriseBag());
    }
}
