package br.com.seudominio.perishrescue.domain.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for {@code Beneficiary}. {@code password} carries the raw password on the
 * way in (register); {@link br.com.seudominio.perishrescue.domain.mappers.DTOMapper}
 * implementations must never populate it on the way out, so a beneficiary's
 * password/hash is never returned to a client.
 *
 * @param id    the beneficiary's identifier
 * @param name     the beneficiary's name
 * @param document the beneficiary's CPF, with or without mask
 * @param email the beneficiary's login email
 * @param password the raw password (register input only; always {@code null} on output)
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public record BeneficiaryDTO(
        Long id,
        @NotBlank String name,
        @NotBlank String document,
        @NotBlank @Email String email,
        @NotBlank String password) {
}
