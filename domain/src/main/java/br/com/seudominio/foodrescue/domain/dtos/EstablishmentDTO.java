package br.com.seudominio.foodrescue.domain.dtos;

import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for {@code Establishment}. {@code password} carries the raw password on
 * the way in (register); {@link br.com.seudominio.foodrescue.domain.mappers.DTOMapper}
 * implementations must never populate it on the way out, so an establishment's
 * password/hash is never returned to a client.
 *
 * @param id       the establishment's identifier
 * @param name     the establishment's name
 * @param cnpj     the establishment's CNPJ, with or without mask
 * @param address  the establishment's address
 * @param category the establishment's business category
 * @param email    the establishment's login email
 * @param password the raw password (register input only; always {@code null} on output)
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record EstablishmentDTO(
        Long id,
        @NotBlank String name,
        @NotBlank String cnpj,
        @NotBlank String address,
        @NotNull EstablishmentCategory category,
        @NotBlank @Email String email,
        @NotBlank String password) {
}
