package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.domain.entities.AbstractEntity;
import br.com.seudominio.foodrescue.domain.mappers.DTOMapper;
import br.com.seudominio.foodrescue.persistence.repositories.GenericRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Generic Service - This class is responsible for the common business operations of the entities.
 *
 * @param <E> the entity type.
 * @param <D> the DTO type.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public abstract class GenericService<E extends AbstractEntity, D> {

    protected final GenericRepository<E> repository;
    protected final DTOMapper<E, D> dtoMapper;
    protected final Validator validator;
    protected final MessageUtils messageUtils;

    /**
     * Constructor-based dependency injection.
     *
     * @param repository   the generic repository for entity operations
     * @param dtoMapper    the DTO mapper for converting entities to DTOs and vice versa
     * @param validator    the validator for validating entities
     * @param messageUtils the utility for retrieving error messages
     */
    protected GenericService(GenericRepository<E> repository, DTOMapper<E, D> dtoMapper, Validator validator, MessageUtils messageUtils) {
        this.repository = repository;
        this.dtoMapper = dtoMapper;
        this.validator = validator;
        this.messageUtils = messageUtils;
    }

    /**
     * Saves the given DTO by converting it to an entity, validating it, and then saving it to the repository.
     *
     * @param dto The DTO to be saved.
     * @return The saved DTO.
     * @throws ConstraintViolationException if the entity violates any constraints.
     */
    public D save(D dto) {
        E entity = dtoMapper.toEntity(dto);
        Set<ConstraintViolation<E>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(
                    messageUtils.getMessage("error.entity.constraint.violation"), violations);
        }
        return dtoMapper.toDto(this.repository.save(entity));
    }

    /**
     * Updates the given DTO by converting it to an entity, validating it, and then saving it to the repository.
     *
     * @param id  The id of the entity to be updated.
     * @param dto The DTO to be updated.
     * @return The updated DTO.
     * @throws ConstraintViolationException if the entity violates any constraints.
     * @throws IllegalArgumentException     if the entity is not found in the repository.
     */
    public D update(Long id, D dto) {
        Optional<E> objectOpt = this.repository.findById(id);
        if (objectOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    messageUtils.getMessage("error.entity.not.found", id));
        }

        E entity = dtoMapper.toEntity(dto);
        Set<ConstraintViolation<E>> violations = validator.validate(entity);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(
                    messageUtils.getMessage("error.entity.constraint.violation"), violations);
        }
        return dtoMapper.toDto(this.repository.save(entity));
    }

    /**
     * Save all entities.
     *
     * @param dtos the entities.
     * @return the saved entities.
     */
    public List<D> saveAll(List<D> dtos) {
        dtos.forEach(this::save);
        return dtos;
    }

    /**
     * Deletes the given entity.
     *
     * @param entity the entity to delete
     */
    public void delete(D entity) {
        this.repository.delete(dtoMapper.toEntity(entity));
    }

    /**
     * Deletes the entity by its id.
     *
     * @param id the id of the entity to delete
     * @throws IllegalArgumentException if the entity is not found in the repository.
     */
    public void deleteById(Long id) {
        E entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(messageUtils.getMessage("error.entity.not.found", id)));
        entity.setActive(false);
        repository.save(entity);
    }

    /**
     * Find by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    public D findById(Long id) {
        return repository.findById(id)
                .map(dtoMapper::toDto)
                .orElse(null);
    }

    /**
     * Find by id and active.
     *
     * @param id     the id.
     * @param active the active flag.
     * @return the entity.
     */
    public D findByIdAndActive(Long id, Boolean active) {
        return repository.findByIdAndActive(id, active)
                .map(dtoMapper::toDto)
                .orElse(null);
    }

    /**
     * Find all entities.
     *
     * @return the entities.
     */
    public List<D> findAll() {
        return repository.findAll().stream().map(dtoMapper::toDto).toList();
    }

    /**
     * Find all entities with pagination.
     *
     * @param pageable the pagination.
     * @return the entities.
     */
    public Page<D> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(dtoMapper::toDto);
    }

    /**
     * Reactivates an entity by its id.
     *
     * @param id the id of the entity to reactivate
     */
    public void reactive(Long id) {
        this.repository.reactive(id);
    }

    protected Validator getValidator() {
        return validator;
    }

    protected <T> void updateFieldIfPresent(T value, Consumer<T> setter) {
        if (value != null) {
            setter.accept(value);
        }
    }
}
