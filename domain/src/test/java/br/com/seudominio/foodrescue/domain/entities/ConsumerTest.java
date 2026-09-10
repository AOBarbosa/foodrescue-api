package br.com.seudominio.foodrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConsumerTest {

    @Test
    void buildsConsumerWithAllFields() {
        Consumer consumer = Consumer.builder()
                .name("Maria")
                .email("maria@example.com")
                .passwordHash("hashed-password")
                .build();

        assertThat(consumer.getName()).isEqualTo("Maria");
        assertThat(consumer.getEmail()).isEqualTo("maria@example.com");
        assertThat(consumer.getPasswordHash()).isEqualTo("hashed-password");
    }
}
