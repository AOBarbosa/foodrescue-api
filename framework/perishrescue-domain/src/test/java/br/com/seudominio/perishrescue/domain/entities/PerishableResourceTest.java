package br.com.seudominio.perishrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

class PerishableResourceTest {

    private Offerer anyOfferer() {
        return Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();
    }

    @Test
    void buildsResourceWithAllFields() {
        PerishableResource resource = TestResource.builder()
                .offerer(anyOfferer())
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .stockQuantity(10)
                .build();

        assertThat(resource.getName()).isEqualTo("Pão francês");
        assertThat(resource.getCategory()).isEqualTo("bakery");
        assertThat(resource.getOriginalPrice()).isEqualTo(Money.of(5.0));
        assertThat(resource.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void defaultsCurrentPriceToOriginalPriceWhenNotSetExplicitly() {
        PerishableResource resource = TestResource.builder()
                .offerer(anyOfferer())
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .build();

        assertThat(resource.getCurrentPrice()).isEqualTo(resource.getOriginalPrice());
    }

    @Test
    void keepsCurrentPriceWhenSetExplicitly() {
        PerishableResource resource = TestResource.builder()
                .offerer(anyOfferer())
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .currentPrice(Money.of(4.0))
                .build();

        assertThat(resource.getCurrentPrice()).isEqualTo(Money.of(4.0));
    }
}
