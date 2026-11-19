package br.com.seudominio.perishrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;


import org.junit.jupiter.api.Test;

class OffererTest {

    @Test
    void buildsOffererWithAllFields() {
        Offerer offerer = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();

        assertThat(offerer.getName()).isEqualTo("Padaria da Esquina");
        assertThat(offerer.getCnpj()).isEqualTo("12345678000199");
        assertThat(offerer.getAddress()).isEqualTo("Rua das Flores, 123");
        assertThat(offerer.getCategory()).isEqualTo("BAKERY");
        assertThat(offerer.getEmail()).isEqualTo("padaria@example.com");
        assertThat(offerer.getPasswordHash()).isEqualTo("hashed-password");
    }
}
