package br.com.seudominio.foodrescue.domain.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import java.io.Serializable;

/**
 * Custom Envers revision entity, backed by an explicit sequence (like every
 * other entity in this project) instead of the default generator Envers
 * would otherwise pick, so the generated schema is predictable under
 * {@code ddl-auto=validate}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@RevisionEntity
@Table(name = "revinfo")
public class AuditRevisionEntity implements Serializable {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REVINFO")
    @SequenceGenerator(name = "SEQ_REVINFO", sequenceName = "seq_revinfo", allocationSize = 1)
    @RevisionNumber
    private int id;

    /**
     * Timestamp (epoch millis) the revision was created.
     */
    @RevisionTimestamp
    private long timestamp;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
