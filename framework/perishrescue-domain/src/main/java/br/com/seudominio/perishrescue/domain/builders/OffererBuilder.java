package br.com.seudominio.perishrescue.domain.builders;

import br.com.seudominio.perishrescue.domain.entities.Offerer;

import java.time.LocalTime;

/**
 * Builder class for creating instances of {@link Offerer}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class OffererBuilder {

    private Long id;
    private String name;
    private String cnpj;
    private String address;
    private String category;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private String email;
    private String passwordHash;

    /**
     * Sets the ID of the offerer.
     *
     * @param id the ID of the offerer
     * @return the current instance of {@link OffererBuilder}
     */
    public OffererBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the name of the offerer.
     *
     * @param name the name of the offerer
     * @return the current instance of {@link OffererBuilder}
     */
    public OffererBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the CNPJ of the offerer.
     *
     * @param cnpj the CNPJ of the offerer
     * @return the current instance of {@link OffererBuilder}
     */
    public OffererBuilder cnpj(String cnpj) {
        this.cnpj = cnpj;
        return this;
    }

    /**
     * Sets the address of the offerer.
     *
     * @param address the address of the offerer
     * @return the current instance of {@link OffererBuilder}
     */
    public OffererBuilder address(String address) {
        this.address = address;
        return this;
    }

    /**
     * Sets the business category of the offerer.
     *
     * @param category the business category of the offerer
     * @return the current instance of {@link OffererBuilder}
     */
    public OffererBuilder category(String category) {
        this.category = category;
        return this;
    }

    /**
     * Sets the daily opening and closing times of the offerer.
     *
     * @param openingTime the time the offerer opens
     * @param closingTime the time the offerer closes
     * @return the current instance of {@link OffererBuilder}
     */
    public OffererBuilder businessHours(LocalTime openingTime, LocalTime closingTime) {
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        return this;
    }

    /**
     * Sets the login email of the offerer.
     *
     * @param email the login email of the offerer
     * @return the current instance of {@link OffererBuilder}
     */
    public OffererBuilder email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Sets the hashed password of the offerer.
     *
     * @param passwordHash the hashed password of the offerer
     * @return the current instance of {@link OffererBuilder}
     */
    public OffererBuilder passwordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    /**
     * Builds and returns an instance of {@link Offerer} with the set properties.
     *
     * @return a new instance of {@link Offerer}
     */
    public Offerer build() {
        Offerer offerer = new Offerer();
        offerer.setId(id);
        offerer.setName(name);
        offerer.setCnpj(cnpj);
        offerer.setAddress(address);
        offerer.setCategory(category);
        offerer.setOpeningTime(openingTime);
        offerer.setClosingTime(closingTime);
        offerer.setEmail(email);
        offerer.setPasswordHash(passwordHash);
        return offerer;
    }
}
