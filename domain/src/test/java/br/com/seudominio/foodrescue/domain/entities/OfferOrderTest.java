package br.com.seudominio.foodrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;
import br.com.seudominio.foodrescue.domain.enums.OfferOrderStatus;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class OfferOrderTest {

    private Offer anyOffer() {
        Establishment establishment = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();

        Product product = Product.builder()
                .establishment(establishment)
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .stockQuantity(10)
                .build();

        return Offer.builder()
                .product(product)
                .discountedPrice(Money.of(3.0))
                .availableQuantity(5)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();
    }

    private Consumer anyConsumer() {
        return Consumer.builder()
                .name("Maria")
                .email("maria@example.com")
                .passwordHash("hashed-password")
                .build();
    }

    @Test
    void defaultsStatusToReserved() {
        OfferOrder offerOrder = OfferOrder.builder()
                .offer(anyOffer())
                .consumer(anyConsumer())
                .quantity(1)
                .build();

        assertThat(offerOrder.getStatus()).isEqualTo(OfferOrderStatus.RESERVED);
    }

    @Test
    void buildsOfferOrderWithAllFields() {
        Offer offer = anyOffer();
        Consumer consumer = anyConsumer();

        OfferOrder offerOrder = OfferOrder.builder()
                .offer(offer)
                .consumer(consumer)
                .quantity(2)
                .build();

        assertThat(offerOrder.getOffer()).isEqualTo(offer);
        assertThat(offerOrder.getConsumer()).isEqualTo(consumer);
        assertThat(offerOrder.getQuantity()).isEqualTo(2);
    }
}
