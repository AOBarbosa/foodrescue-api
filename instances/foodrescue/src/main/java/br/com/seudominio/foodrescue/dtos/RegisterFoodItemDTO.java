package br.com.seudominio.foodrescue.dtos;

import br.com.seudominio.foodrescue.domain.Allergen;
import br.com.seudominio.foodrescue.domain.StorageTemperature;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Request to register a {@code FoodItem}. Stock and expiration date start
 * empty and are set through the inventory endpoint.
 *
 * @param name               the item's name
 * @param category           the item's category (e.g. "bread", "lunch box")
 * @param originalPrice      the price before any discount
 * @param photoUrl           an optional photo URL
 * @param allergens          the allergens it contains; null or empty for none
 * @param storageTemperature the temperature range it must be kept in
 * @param surpriseBag        whether it is a surprise bag
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record RegisterFoodItemDTO(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @Positive BigDecimal originalPrice,
        String photoUrl,
        Set<Allergen> allergens,
        @NotNull StorageTemperature storageTemperature,
        boolean surpriseBag) {
}
