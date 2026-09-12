package br.com.seudominio.foodrescue.domain.builders;

import br.com.seudominio.foodrescue.domain.entities.Consumer;

/**
 * Builder class for creating instances of {@link Consumer}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class ConsumerBuilder {

    private Long id;
    private String name;
    private String email;
    private String passwordHash;

    /**
     * Sets the ID of the consumer.
     *
     * @param id the ID of the consumer
     * @return the current instance of {@link ConsumerBuilder}
     */
    public ConsumerBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the name of the consumer.
     *
     * @param name the name of the consumer
     * @return the current instance of {@link ConsumerBuilder}
     */
    public ConsumerBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the login email of the consumer.
     *
     * @param email the login email of the consumer
     * @return the current instance of {@link ConsumerBuilder}
     */
    public ConsumerBuilder email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Sets the hashed password of the consumer.
     *
     * @param passwordHash the hashed password of the consumer
     * @return the current instance of {@link ConsumerBuilder}
     */
    public ConsumerBuilder passwordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    /**
     * Builds and returns an instance of {@link Consumer} with the set properties.
     *
     * @return a new instance of {@link Consumer}
     */
    public Consumer build() {
        Consumer consumer = new Consumer();
        consumer.setId(id);
        consumer.setName(name);
        consumer.setEmail(email);
        consumer.setPasswordHash(passwordHash);
        return consumer;
    }
}
