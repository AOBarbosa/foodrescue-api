package br.com.seudominio.foodrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;

import org.junit.jupiter.api.Test;

class EstablishmentTest {

    @Test
    void buildsEstablishmentWithAllFields() {
        Establishment establishment = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();

        assertThat(establishment.getName()).isEqualTo("Padaria da Esquina");
        assertThat(establishment.getCnpj()).isEqualTo("12345678000199");
        assertThat(establishment.getAddress()).isEqualTo("Rua das Flores, 123");
        assertThat(establishment.getCategory()).isEqualTo(EstablishmentCategory.BAKERY);
        assertThat(establishment.getEmail()).isEqualTo("padaria@example.com");
        assertThat(establishment.getPasswordHash()).isEqualTo("hashed-password");
    }
}
