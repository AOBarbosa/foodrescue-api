package br.com.seudominio.perishrescue.domain.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * DTO for updating an {@code Offerer}'s own profile. Unlike
 * {@link OffererDTO}, {@code password} is optional here: a blank or
 * {@code null} value leaves the current password hash untouched, a non-blank
 * value is hashed and replaces it.
 *
 * @param name     the offerer's name
 * @param cnpj     the offerer's CNPJ, with or without mask
 * @param address  the offerer's address
 * @param category    the offerer's business category
 * @param openingTime the time the offerer opens every day
 * @param closingTime the time the offerer closes every day
 * @param email    the offerer's login email
 * @param password the new raw password, or {@code null}/blank to keep the current one
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record OffererUpdateDTO(
        @NotBlank String name,
        @NotBlank String cnpj,
        @NotBlank String address,
        @NotBlank String category,
        @NotNull LocalTime openingTime,
        @NotNull LocalTime closingTime,
        @NotBlank @Email String email,
        String password) {
}
