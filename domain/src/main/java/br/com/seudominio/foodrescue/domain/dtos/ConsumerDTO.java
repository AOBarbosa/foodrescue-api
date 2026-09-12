package br.com.seudominio.foodrescue.domain.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for {@code Consumer}. {@code password} carries the raw password on the
 * way in (register); {@link br.com.seudominio.foodrescue.domain.mappers.DTOMapper}
 * implementations must never populate it on the way out, so a consumer's
 * password/hash is never returned to a client.
 *
 * @param id    the consumer's identifier
 * @param name  the consumer's name
 * @param email the consumer's login email
 * @param password the raw password (register input only; always {@code null} on output)
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record ConsumerDTO(
        Long id,
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password) {
}
