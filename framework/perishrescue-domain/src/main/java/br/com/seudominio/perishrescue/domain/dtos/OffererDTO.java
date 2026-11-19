package br.com.seudominio.perishrescue.domain.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * DTO for {@code Offerer}. {@code password} carries the raw password on
 * the way in (register); {@link br.com.seudominio.perishrescue.domain.mappers.DTOMapper}
 * implementations must never populate it on the way out, so an offerer's
 * password/hash is never returned to a client.
 *
 * @param id       the offerer's identifier
 * @param name     the offerer's name
 * @param cnpj     the offerer's CNPJ, with or without mask
 * @param address  the offerer's address
 * @param category    the offerer's business category
 * @param openingTime the time the offerer opens every day
 * @param closingTime the time the offerer closes every day
 * @param email    the offerer's login email
 * @param password the raw password (register input only; always {@code null} on output)
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record OffererDTO(
        Long id,
        @NotBlank String name,
        @NotBlank String cnpj,
        @NotBlank String address,
        @NotBlank String category,
        @NotNull LocalTime openingTime,
        @NotNull LocalTime closingTime,
        @NotBlank @Email String email,
        @NotBlank String password) {
}
