package br.com.seudominio.foodrescue.domain.builders;

import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;

/**
 * Builder class for creating instances of {@link Establishment}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class EstablishmentBuilder {

    private Long id;
    private String name;
    private String cnpj;
    private String address;
    private EstablishmentCategory category;
    private String email;
    private String passwordHash;

    /**
     * Sets the ID of the establishment.
     *
     * @param id the ID of the establishment
     * @return the current instance of {@link EstablishmentBuilder}
     */
    public EstablishmentBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the name of the establishment.
     *
     * @param name the name of the establishment
     * @return the current instance of {@link EstablishmentBuilder}
     */
    public EstablishmentBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the CNPJ of the establishment.
     *
     * @param cnpj the CNPJ of the establishment
     * @return the current instance of {@link EstablishmentBuilder}
     */
    public EstablishmentBuilder cnpj(String cnpj) {
        this.cnpj = cnpj;
        return this;
    }

    /**
     * Sets the address of the establishment.
     *
     * @param address the address of the establishment
     * @return the current instance of {@link EstablishmentBuilder}
     */
    public EstablishmentBuilder address(String address) {
        this.address = address;
        return this;
    }

    /**
     * Sets the business category of the establishment.
     *
     * @param category the business category of the establishment
     * @return the current instance of {@link EstablishmentBuilder}
     */
    public EstablishmentBuilder category(EstablishmentCategory category) {
        this.category = category;
        return this;
    }

    /**
     * Sets the login email of the establishment.
     *
     * @param email the login email of the establishment
     * @return the current instance of {@link EstablishmentBuilder}
     */
    public EstablishmentBuilder email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Sets the hashed password of the establishment.
     *
     * @param passwordHash the hashed password of the establishment
     * @return the current instance of {@link EstablishmentBuilder}
     */
    public EstablishmentBuilder passwordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    /**
     * Builds and returns an instance of {@link Establishment} with the set properties.
     *
     * @return a new instance of {@link Establishment}
     */
    public Establishment build() {
        Establishment establishment = new Establishment();
        establishment.setId(id);
        establishment.setName(name);
        establishment.setCnpj(cnpj);
        establishment.setAddress(address);
        establishment.setCategory(category);
        establishment.setEmail(email);
        establishment.setPasswordHash(passwordHash);
        return establishment;
    }
}
