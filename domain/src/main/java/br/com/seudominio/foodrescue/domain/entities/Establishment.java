package br.com.seudominio.foodrescue.domain.entities;

import br.com.seudominio.foodrescue.domain.builders.EstablishmentBuilder;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

/**
 * Establishment. This class represents an establishment (bakery, restaurant,
 * market or snack bar) that registers on the platform to manage its own
 * products, stock, sales and offers.
 * CNPJ format validation belongs to UC01, not to this class.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "establishments")
@SuppressWarnings("serial")
public class Establishment extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ESTABLISHMENT")
    @SequenceGenerator(name = "SEQ_ESTABLISHMENT", sequenceName = "seq_establishment", allocationSize = 1)
    private Long id;

    /**
     * Name of the establishment.
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * CNPJ of the establishment.
     */
    @NotBlank
    @Column(nullable = false, unique = true)
    private String cnpj;

    /**
     * Address of the establishment.
     */
    @NotBlank
    @Column(nullable = false)
    private String address;

    /**
     * Business category of the establishment.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstablishmentCategory category;

    /**
     * Login email of the establishment.
     */
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hashed password of the establishment.
     */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Default constructor.
     */
    public Establishment() {
        super();
    }

    /**
     * Returns a new instance of the EstablishmentBuilder for building Establishment objects.
     *
     * @return A new EstablishmentBuilder instance.
     */
    public static EstablishmentBuilder builder() {
        return new EstablishmentBuilder();
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

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public EstablishmentCategory getCategory() {
        return category;
    }

    public void setCategory(EstablishmentCategory category) {
        this.category = category;
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
        return "Establishment [id=" + id + ", name=" + name + ", cnpj=" + cnpj + "]";
    }
}
