package br.com.seudominio.foodrescue.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for {@code Product}.
 *
 * @param id                the product's identifier
 * @param name              the product's name
 * @param category          the product's category
 * @param originalPrice     the product's original price
 * @param currentPrice      the product's current price
 * @param photoUrl          the product's photo URL
 * @param stockQuantity     the product's stock quantity
 * @param expirationDate    the product's expiration date
 * @param establishmentId   the product's establishment identifier
 * @param modificationDate  the date and time of the product's last update
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record ProductDTO(
        Long id,
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @Positive BigDecimal originalPrice,
        BigDecimal currentPrice,
        String photoUrl,
        int stockQuantity,
        LocalDate expirationDate,
        Long establishmentId,
        LocalDateTime modificationDate) {
}
