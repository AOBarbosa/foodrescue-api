package br.com.seudominio.foodrescue.persistence.repositories;

import br.com.seudominio.foodrescue.domain.entities.AbstractEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository - This class is responsible for the common database operations of the entities.
 *
 * @param <T> the entity type.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@NoRepositoryBean
public interface GenericRepository<T extends AbstractEntity> extends JpaRepository<T, Long> {

    /**
     * Find all active entities.
     *
     * @return a list of active entities.
     */
    @NonNull
    @Override
    @Query(value = "select e from #{#entityName} e where e.active = true")
    List<T> findAll();

    /**
     * Find all active entities with pagination.
     *
     * @param pageable the pagination information.
     * @return a page of active entities.
     */
    @NonNull
    @Override
    @Query(value = "select e from #{#entityName} e where e.active = true")
    Page<T> findAll(@NonNull Pageable pageable);

    /**
     * Find an active entity by its ID.
     *
     * @param id the ID of the entity.
     * @return an optional containing the active entity if found, or empty if not found.
     */
    @NonNull
    @Override
    @Query(value = "select e from #{#entityName} e where e.active = true and e.id = ?1")
    Optional<T> findById(@NonNull Long id);

    /**
     * Find an entity by its ID and active status.
     *
     * @param id     the ID of the entity.
     * @param active the active status of the entity.
     * @return an optional containing the entity if found, or empty if not found.
     */
    @Query(value = "select e from #{#entityName} e where e.active = ?2 and e.id = ?1")
    Optional<T> findByIdAndActive(Long id, Boolean active);

    /**
     * Find all entities, including active and inactive.
     *
     * @return a list of all entities.
     */
    @Query(value = "select e from #{#entityName} e")
    List<T> findAllActiveAndInactive();

    /**
     * Find all entities, including active and inactive, with pagination.
     *
     * @param pageable the pagination information.
     * @return a page of all entities.
     */
    @Query(value = "select e from #{#entityName} e")
    Page<T> findAllActiveAndInactive(Pageable pageable);

    /**
     * Soft delete an entity by its ID by setting its active status to false.
     *
     * @param id the ID of the entity to delete.
     */
    @Override
    default void deleteById(@NonNull Long id) {
        Optional<T> entity = findById(id);
        if (entity.isPresent()) {
            entity.get().setActive(false);
            save(entity.get());
        }
    }

    /**
     * Soft delete an entity by setting its active status to false.
     *
     * @param entity the entity to delete.
     */
    @Override
    default void delete(T entity) {
        entity.setActive(false);
        save(entity);
    }

    /**
     * Soft delete all entities in the given iterable by setting their active status to false.
     *
     * @param entities the entities to delete.
     */
    @Override
    default void deleteAll(Iterable<? extends T> entities) {
        entities.forEach(entity -> entity.setActive(false));
        saveAll(entities);
    }

    /**
     * Reactivate an entity by its ID by setting its active status to true.
     *
     * @param id the ID of the entity to reactivate.
     */
    default void reactive(Long id) {
        Optional<T> entity = findByIdAndActive(id, false);
        if (entity.isPresent()) {
            T entityToReactive = entity.get();
            entityToReactive.setActive(true);
            save(entityToReactive);
        }
    }

    /**
     * Count the active entities.
     *
     * @return the number of active entities.
     */
    @Query(value = "select count(e) from #{#entityName} e where e.active = true")
    Long countActive();
}
