package br.com.seudominio.foodrescue.domain.mappers;

/**
 * Mapper contract between an entity and its DTO representation.
 *
 * @param <E> the entity type.
 * @param <D> the DTO type.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public interface DTOMapper<E, D> {

    /**
     * Converts a DTO to its entity representation.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity
     */
    E toEntity(D dto);

    /**
     * Converts an entity to its DTO representation.
     *
     * @param entity the entity to convert
     * @return the corresponding DTO
     */
    D toDto(E entity);
}
