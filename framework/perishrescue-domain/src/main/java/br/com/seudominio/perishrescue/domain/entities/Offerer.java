package br.com.seudominio.perishrescue.domain.entities;

import br.com.seudominio.perishrescue.domain.builders.OffererBuilder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import java.time.LocalTime;

/**
 * Offerer. One of the two universal actors of the PerishRescue framework:
 * the establishment (bakery, pharmacy, flower shop...) that registers on the
 * platform to manage its own perishable resources, stock, sales and offers.
 * CNPJ format validation belongs to UC01, not to this class.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "offerers")
@SuppressWarnings("serial")
public class Offerer extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OFFERER")
    @SequenceGenerator(name = "SEQ_OFFERER", sequenceName = "seq_offerer", allocationSize = 1)
    private Long id;

    /**
     * Name of the offerer.
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * CNPJ of the offerer.
     */
    @NotBlank
    @Column(nullable = false, unique = true)
    private String cnpj;

    /**
     * Address of the offerer.
     */
    @NotBlank
    @Column(nullable = false)
    private String address;

    /**
     * Business category of the offerer. Free text in the kernel: each
     * instance decides which categories it accepts (e.g. bakery, pharmacy).
     */
    @NotBlank
    @Column(nullable = false)
    private String category;

    /**
     * Time the offerer opens every day; pickups happen between this and
     * {@link #closingTime}.
     */
    @NotNull
    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    /**
     * Time the offerer closes every day.
     */
    @NotNull
    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    /**
     * Login email of the offerer.
     */
    @NotBlank
    @Email
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * Hashed password of the offerer.
     */
    @NotBlank
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Default constructor.
     */
    public Offerer() {
        super();
    }

    /**
     * Returns a new instance of the OffererBuilder for building Offerer objects.
     *
     * @return A new OffererBuilder instance.
     */
    public static OffererBuilder builder() {
        return new OffererBuilder();
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalTime openingTime) {
        this.openingTime = openingTime;
    }

    public LocalTime getClosingTime() {
        return closingTime;
    }

    public void setClosingTime(LocalTime closingTime) {
        this.closingTime = closingTime;
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
        return "Offerer [id=" + id + ", name=" + name + ", cnpj=" + cnpj + "]";
    }
}
