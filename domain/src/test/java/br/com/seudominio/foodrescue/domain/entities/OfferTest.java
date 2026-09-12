package br.com.seudominio.foodrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;
import br.com.seudominio.foodrescue.domain.enums.OfferStatus;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class OfferTest {

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
    void defaultsStatusToActiveAndPriorityToFalse() {
        Offer offer = Offer.builder()
                .product(anyProduct())
                .discountedPrice(Money.of(3.0))
                .availableQuantity(5)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();

        assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACTIVE);
        assertThat(offer.isPriority()).isFalse();
    }

    @Test
    void buildsOfferWithAllFields() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        Offer offer = Offer.builder()
                .product(anyProduct())
                .discountedPrice(Money.of(3.0))
                .availableQuantity(5)
                .expiresAt(expiresAt)
                .priority(true)
                .build();

        assertThat(offer.getDiscountedPrice()).isEqualTo(Money.of(3.0));
        assertThat(offer.getAvailableQuantity()).isEqualTo(5);
        assertThat(offer.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(offer.isPriority()).isTrue();
    }
}
