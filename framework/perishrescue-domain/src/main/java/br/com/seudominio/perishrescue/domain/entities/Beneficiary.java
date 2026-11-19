package br.com.seudominio.perishrescue.domain.entities;

import br.com.seudominio.perishrescue.domain.builders.BeneficiaryBuilder;

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
 * Beneficiary. This class represents a beneficiary that registers on the platform
 * to browse and reserve offers (UC09).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "beneficiaries")
@SuppressWarnings("serial")
public class Beneficiary extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BENEFICIARY")
    @SequenceGenerator(name = "SEQ_BENEFICIARY", sequenceName = "seq_beneficiary", allocationSize = 1)
    private Long id;

    /**
     * Name of the beneficiary.
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * CPF of the beneficiary, digits only. Unique: rescue quotas are
     * counted per CPF.
     */
    @NotBlank
    @Column(nullable = false, unique = true, length = 11)
    private String document;

    /**
     * Login email of the beneficiary.
     */
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hashed password of the beneficiary.
     */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Default constructor.
     */
    public Beneficiary() {
        super();
    }

    /**
     * Returns a new instance of the BeneficiaryBuilder for building Beneficiary objects.
     *
     * @return A new BeneficiaryBuilder instance.
     */
    public static BeneficiaryBuilder builder() {
        return new BeneficiaryBuilder();
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

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
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
        return "Beneficiary [id=" + id + ", name=" + name + ", email=" + email + "]";
    }
}
