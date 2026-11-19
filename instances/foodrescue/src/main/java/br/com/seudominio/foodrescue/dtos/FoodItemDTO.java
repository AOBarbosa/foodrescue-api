package br.com.seudominio.foodrescue.dtos;

import br.com.seudominio.foodrescue.domain.Allergen;
import br.com.seudominio.foodrescue.domain.StorageTemperature;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Response view of a {@code FoodItem}: the kernel's common resource fields
 * plus the food-specific ones.
 *
 * @param id                 the item's identifier
 * @param name               the item's name
 * @param category           the item's category
 * @param originalPrice      the price before any discount
 * @param currentPrice       the current price, reflecting an accepted discount
 * @param photoUrl           the photo URL
 * @param stockQuantity      the units in stock
 * @param expirationDate     the declared expiration date
 * @param offererId          the owning offerer's identifier
 * @param modificationDate   the last modification moment
 * @param allergens          the allergens it contains
 * @param storageTemperature the temperature range it must be kept in
 * @param surpriseBag        whether it is a surprise bag
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record FoodItemDTO(
        Long id,
        String name,
        String category,
        BigDecimal originalPrice,
        BigDecimal currentPrice,
        String photoUrl,
        int stockQuantity,
        LocalDate expirationDate,
        Long offererId,
        LocalDateTime modificationDate,
        Set<Allergen> allergens,
        StorageTemperature storageTemperature,
        boolean surpriseBag) {
}
