package br.com.seudominio.perishrescue.domain.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for {@code PerishableResource}.
 *
 * @param id                the resource's identifier
 * @param name              the resource's name
 * @param category          the resource's category
 * @param originalPrice     the resource's original price
 * @param currentPrice      the resource's current price
 * @param photoUrl          the resource's photo URL
 * @param stockQuantity     the resource's stock quantity
 * @param expirationDate    the resource's expiration date
 * @param offererId   the resource's offerer identifier
 * @param modificationDate  the date and time of the resource's last update
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record ResourceDTO(
        Long id,
        @NotBlank String name,
        @NotBlank String category,
        @NotNull @Positive BigDecimal originalPrice,
        BigDecimal currentPrice,
        String photoUrl,
        int stockQuantity,
        LocalDate expirationDate,
        Long offererId,
        LocalDateTime modificationDate) {
}
