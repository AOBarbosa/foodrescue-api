package br.com.seudominio.perishrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

class SaleTest {

    private PerishableResource anyResource() {
        Offerer offerer = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();

        return TestResource.builder()
                .offerer(offerer)
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .stockQuantity(10)
                .build();
    }

    @Test
    void buildsSaleWithAllFields() {
        LocalDateTime soldAt = LocalDateTime.now();

        Sale sale = Sale.builder()
                .resource(anyResource())
                .quantity(2)
                .unitPrice(Money.of(5.0))
                .soldAt(soldAt)
                .build();

        assertThat(sale.getQuantity()).isEqualTo(2);
        assertThat(sale.getUnitPrice()).isEqualTo(Money.of(5.0));
        assertThat(sale.getSoldAt()).isEqualTo(soldAt);
    }
}
