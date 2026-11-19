package br.com.seudominio.foodrescue.domain;

/**
 * Temperature range a {@link FoodItem} must be kept in until it is picked up.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public enum StorageTemperature {

    /** Room temperature (breads, cakes, snacks). */
    AMBIENT,

    /** Refrigerated (dairy, salads, desserts). */
    REFRIGERATED,

    /** Kept warm (ready meals). */
    HEATED
}
