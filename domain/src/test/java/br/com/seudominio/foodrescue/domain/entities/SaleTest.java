package br.com.seudominio.foodrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class SaleTest {

    private Product anyProduct() {
        Establishment establishment = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();

        return Product.builder()
                .establishment(establishment)
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
                .product(anyProduct())
                .quantity(2)
                .unitPrice(Money.of(5.0))
                .soldAt(soldAt)
                .build();

        assertThat(sale.getQuantity()).isEqualTo(2);
        assertThat(sale.getUnitPrice()).isEqualTo(Money.of(5.0));
        assertThat(sale.getSoldAt()).isEqualTo(soldAt);
    }
}
