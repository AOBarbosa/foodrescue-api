package br.com.seudominio.foodrescue.domain.dtos;

import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating an {@code Establishment}'s own profile. Unlike
 * {@link EstablishmentDTO}, {@code password} is optional here: a blank or
 * {@code null} value leaves the current password hash untouched, a non-blank
 * value is hashed and replaces it.
 *
 * @param name     the establishment's name
 * @param cnpj     the establishment's CNPJ, with or without mask
 * @param address  the establishment's address
 * @param category the establishment's business category
 * @param email    the establishment's login email
 * @param password the new raw password, or {@code null}/blank to keep the current one
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record EstablishmentUpdateDTO(
        @NotBlank String name,
        @NotBlank String cnpj,
        @NotBlank String address,
        @NotNull EstablishmentCategory category,
        @NotBlank @Email String email,
        String password) {
}
