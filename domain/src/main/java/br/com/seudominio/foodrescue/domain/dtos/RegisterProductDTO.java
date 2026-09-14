package br.com.seudominio.foodrescue.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * DTO for registering a new product.
 *
 * @param name          the product name
 * @param category      the product category
 * @param originalPrice the product original price
 * @param photoUrl      the product photo URL
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record RegisterProductDTO(
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @Positive BigDecimal originalPrice,
        String photoUrl) {
}
