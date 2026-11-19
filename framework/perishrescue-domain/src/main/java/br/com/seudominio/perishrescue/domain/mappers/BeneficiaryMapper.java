package br.com.seudominio.perishrescue.domain.mappers;

import br.com.seudominio.perishrescue.domain.dtos.BeneficiaryDTO;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;

import org.springframework.stereotype.Component;

/**
 * Mapper between {@link Beneficiary} and {@link BeneficiaryDTO}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class BeneficiaryMapper implements DTOMapper<Beneficiary, BeneficiaryDTO> {

    /**
     * Converts a {@link BeneficiaryDTO} to a {@link Beneficiary}. The raw
     * {@code password} is copied into {@code passwordHash} as-is — hashing it
     * is the service's responsibility, done after mapping.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity
     */
    @Override
    public Beneficiary toEntity(BeneficiaryDTO dto) {
        if (dto == null) {
            return null;
        }
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setId(dto.id());
        beneficiary.setName(dto.name());
        beneficiary.setDocument(normalizeDocument(dto.document()));
        beneficiary.setEmail(dto.email());
        beneficiary.setPasswordHash(dto.password());
        return beneficiary;
    }

    private static String normalizeDocument(String document) {
        return document == null ? null : document.replaceAll("\\D", "");
    }

    /**
     * Converts a {@link Beneficiary} to a {@link BeneficiaryDTO}. {@code password}
     * is always {@code null} — the hash is never returned to a client.
     *
     * @param entity the entity to convert
     * @return the corresponding DTO
     */
    @Override
    public BeneficiaryDTO toDto(Beneficiary entity) {
        if (entity == null) {
            return null;
        }
        return new BeneficiaryDTO(entity.getId(), entity.getName(), entity.getDocument(), entity.getEmail(), null);
    }
}
