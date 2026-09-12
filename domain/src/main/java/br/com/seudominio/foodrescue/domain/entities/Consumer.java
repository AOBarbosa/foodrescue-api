package br.com.seudominio.foodrescue.domain.entities;

import br.com.seudominio.foodrescue.domain.builders.ConsumerBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.hibernate.envers.Audited;

/**
 * Consumer. This class represents a consumer that registers on the platform
 * to browse and reserve offers (UC09).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "consumers")
@SuppressWarnings("serial")
public class Consumer extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_CONSUMER")
    @SequenceGenerator(name = "SEQ_CONSUMER", sequenceName = "seq_consumer", allocationSize = 1)
    private Long id;

    /**
     * Name of the consumer.
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * Login email of the consumer.
     */
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hashed password of the consumer.
     */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Default constructor.
     */
    public Consumer() {
        super();
    }

    /**
     * Returns a new instance of the ConsumerBuilder for building Consumer objects.
     *
     * @return A new ConsumerBuilder instance.
     */
    public static ConsumerBuilder builder() {
        return new ConsumerBuilder();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String toString() {
        return "Consumer [id=" + id + ", name=" + name + ", email=" + email + "]";
    }
}
