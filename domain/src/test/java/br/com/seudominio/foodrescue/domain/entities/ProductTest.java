package br.com.seudominio.foodrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;

import org.junit.jupiter.api.Test;

class ProductTest {

    private Establishment anyEstablishment() {
        return Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();
    }

    @Test
    void buildsProductWithAllFields() {
        Product product = Product.builder()
                .establishment(anyEstablishment())
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .stockQuantity(10)
                .build();

        assertThat(product.getName()).isEqualTo("Pão francês");
        assertThat(product.getCategory()).isEqualTo("bakery");
        assertThat(product.getOriginalPrice()).isEqualTo(Money.of(5.0));
        assertThat(product.getStockQuantity()).isEqualTo(10);
    }

    @Test
    void defaultsCurrentPriceToOriginalPriceWhenNotSetExplicitly() {
        Product product = Product.builder()
                .establishment(anyEstablishment())
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .build();

        assertThat(product.getCurrentPrice()).isEqualTo(product.getOriginalPrice());
    }

    @Test
    void keepsCurrentPriceWhenSetExplicitly() {
        Product product = Product.builder()
                .establishment(anyEstablishment())
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .currentPrice(Money.of(4.0))
                .build();

        assertThat(product.getCurrentPrice()).isEqualTo(Money.of(4.0));
    }
}
