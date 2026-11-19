package br.com.seudominio.perishrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BeneficiaryTest {

    @Test
    void buildsBeneficiaryWithAllFields() {
        Beneficiary beneficiary = Beneficiary.builder()
                .name("Maria")
                .document("52998224725")
                .email("maria@example.com")
                .passwordHash("hashed-password")
                .build();

        assertThat(beneficiary.getName()).isEqualTo("Maria");
        assertThat(beneficiary.getEmail()).isEqualTo("maria@example.com");
        assertThat(beneficiary.getPasswordHash()).isEqualTo("hashed-password");
    }
}
