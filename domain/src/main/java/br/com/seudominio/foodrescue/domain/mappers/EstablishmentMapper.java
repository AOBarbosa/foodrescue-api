package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.domain.dtos.EstablishmentDTO;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentUpdateDTO;
import br.com.seudominio.foodrescue.domain.entities.Establishment;

import org.springframework.stereotype.Component;

/**
 * Mapper between {@link Establishment} and {@link EstablishmentDTO}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class EstablishmentMapper implements DTOMapper<Establishment, EstablishmentDTO> {

    /**
     * Converts an {@link EstablishmentDTO} to an {@link Establishment}. The raw
     * {@code password} is copied into {@code passwordHash} as-is — hashing it
     * is the service's responsibility, done after mapping. The CNPJ is
     * normalized to digits only, so a masked or unmasked input is always
     * persisted and compared the same way.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity
     */
    @Override
    public Establishment toEntity(EstablishmentDTO dto) {
        if (dto == null) {
            return null;
        }
        Establishment establishment = new Establishment();
        establishment.setId(dto.id());
        establishment.setName(dto.name());
        establishment.setCnpj(normalizeCnpj(dto.cnpj()));
        establishment.setAddress(dto.address());
        establishment.setCategory(dto.category());
        establishment.setEmail(dto.email());
        establishment.setPasswordHash(dto.password());
        return establishment;
    }

    /**
     * Applies profile changes from an {@link EstablishmentUpdateDTO} onto an
     * existing entity, in place. {@code password} is left to the caller — a
     * blank/absent value should keep the current hash, which this mapper has
     * no way to decide, so it never touches {@code passwordHash}.
     *
     * @param dto    the update data
     * @param entity the existing entity to update
     */
    public void applyUpdate(EstablishmentUpdateDTO dto, Establishment entity) {
        entity.setName(dto.name());
        entity.setCnpj(normalizeCnpj(dto.cnpj()));
        entity.setAddress(dto.address());
        entity.setCategory(dto.category());
        entity.setEmail(dto.email());
    }

    private static String normalizeCnpj(String cnpj) {
        return cnpj == null ? null : cnpj.replaceAll("\\D", "");
    }

    /**
     * Converts an {@link Establishment} to an {@link EstablishmentDTO}.
     * {@code password} is always {@code null} — the hash is never returned to
     * a client.
     *
     * @param entity the entity to convert
     * @return the corresponding DTO
     */
    @Override
    public EstablishmentDTO toDto(Establishment entity) {
        if (entity == null) {
            return null;
        }
        return new EstablishmentDTO(
                entity.getId(),
                entity.getName(),
                entity.getCnpj(),
                entity.getAddress(),
                entity.getCategory(),
                entity.getEmail(),
                null);
    }
}
