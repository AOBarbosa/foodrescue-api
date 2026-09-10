package br.com.seudominio.foodrescue.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


/**
 * Abstract class that represents a generic entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@MappedSuperclass
public abstract class AbstractEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The date and time when the entity was created.
     * This field is automatically populated when the entity is persisted.
     */
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime creationDate;

    /**
     * The date and time when the entity was last modified.
     * This field is automatically updated when the entity is updated.
     */
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime modificationDate;

    /**
     * Indicates whether the entity is active or not (controls the delete operations).
     */
    @Column
    private Boolean active = true;

    /**
     * Constructors.
     */
    protected AbstractEntity() {
        super();
    }

    // Getters and setters
    public abstract Long getId();

    public abstract void setId(Long id);

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getModificationDate() {
        return modificationDate;
    }

    public void setModificationDate(LocalDateTime modificationDate) {
        this.modificationDate = modificationDate;
    }

    @JsonIgnore
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @PreUpdate
    public void preUpdate() {
        this.modificationDate = LocalDateTime.now();
    }

    /**
     * Before persisting the entity, set the creation date and the active flag.
     */
    @PrePersist
    public void prePersist() {
        this.creationDate = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AbstractEntity that = (AbstractEntity) o;
        if (getId() == null) {
            return Objects.isNull(that.getId());
        } else {
            return Objects.nonNull(that.getId()) && Objects.equals(getId(), that.getId());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationDate, modificationDate, active);
    }

}
