package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.domain.dtos.ConsumerDTO;
import br.com.seudominio.foodrescue.domain.entities.Consumer;

import org.springframework.stereotype.Component;

/**
 * Mapper between {@link Consumer} and {@link ConsumerDTO}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class ConsumerMapper implements DTOMapper<Consumer, ConsumerDTO> {

    /**
     * Converts a {@link ConsumerDTO} to a {@link Consumer}. The raw
     * {@code password} is copied into {@code passwordHash} as-is — hashing it
     * is the service's responsibility, done after mapping.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity
     */
    @Override
    public Consumer toEntity(ConsumerDTO dto) {
        if (dto == null) {
            return null;
        }
        Consumer consumer = new Consumer();
        consumer.setId(dto.id());
        consumer.setName(dto.name());
        consumer.setEmail(dto.email());
        consumer.setPasswordHash(dto.password());
        return consumer;
    }

    /**
     * Converts a {@link Consumer} to a {@link ConsumerDTO}. {@code password}
     * is always {@code null} — the hash is never returned to a client.
     *
     * @param entity the entity to convert
     * @return the corresponding DTO
     */
    @Override
    public ConsumerDTO toDto(Consumer entity) {
        if (entity == null) {
            return null;
        }
        return new ConsumerDTO(entity.getId(), entity.getName(), entity.getEmail(), null);
    }
}
