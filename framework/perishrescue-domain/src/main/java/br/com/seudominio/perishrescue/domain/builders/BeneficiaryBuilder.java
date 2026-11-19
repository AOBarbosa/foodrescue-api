package br.com.seudominio.perishrescue.domain.builders;

import br.com.seudominio.perishrescue.domain.entities.Beneficiary;

/**
 * Builder class for creating instances of {@link Beneficiary}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class BeneficiaryBuilder {

    private Long id;
    private String name;
    private String document;
    private String email;
    private String passwordHash;

    /**
     * Sets the ID of the beneficiary.
     *
     * @param id the ID of the beneficiary
     * @return the current instance of {@link BeneficiaryBuilder}
     */
    public BeneficiaryBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the CPF of the beneficiary.
     *
     * @param document the CPF of the beneficiary
     * @return the current instance of {@link BeneficiaryBuilder}
     */
    public BeneficiaryBuilder document(String document) {
        this.document = document;
        return this;
    }

    /**
     * Sets the name of the beneficiary.
     *
     * @param name the name of the beneficiary
     * @return the current instance of {@link BeneficiaryBuilder}
     */
    public BeneficiaryBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the login email of the beneficiary.
     *
     * @param email the login email of the beneficiary
     * @return the current instance of {@link BeneficiaryBuilder}
     */
    public BeneficiaryBuilder email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Sets the hashed password of the beneficiary.
     *
     * @param passwordHash the hashed password of the beneficiary
     * @return the current instance of {@link BeneficiaryBuilder}
     */
    public BeneficiaryBuilder passwordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    /**
     * Builds and returns an instance of {@link Beneficiary} with the set properties.
     *
     * @return a new instance of {@link Beneficiary}
     */
    public Beneficiary build() {
        Beneficiary beneficiary = new Beneficiary();
        beneficiary.setId(id);
        beneficiary.setName(name);
        beneficiary.setDocument(document);
        beneficiary.setEmail(email);
        beneficiary.setPasswordHash(passwordHash);
        return beneficiary;
    }
}
