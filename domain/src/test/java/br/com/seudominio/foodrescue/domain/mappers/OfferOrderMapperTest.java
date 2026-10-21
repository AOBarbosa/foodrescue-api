package br.com.seudominio.foodrescue.domain.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.OfferOrder;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.OfferOrderStatus;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class OfferOrderMapperTest {

    private final OfferOrderMapper mapper = new OfferOrderMapper();

    @Test
    void toDtoMapsAllFieldsAndComputesTotalPrice() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 14, 15, 0);

        Product product = Product.builder()
                .id(10L)
                .name("Pao de queijo")
                .build();

        Offer offer = Offer.builder()
                .id(700L)
                .product(product)
                .discountedPrice(Money.of(new BigDecimal("5.20")))
                .availableQuantity(5)
                .expiresAt(now.plusHours(3))
                .build();

        Consumer consumer = Consumer.builder()
                .id(50L)
                .name("Maria")
                .build();

        OfferOrder order = OfferOrder.builder()
                .id(900L)
                .offer(offer)
                .consumer(consumer)
                .quantity(3)
                .status(OfferOrderStatus.RESERVED)
                .build();
        order.setCreationDate(now);

        OfferOrderDTO dto = mapper.toDto(order);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(900L);
        assertThat(dto.offerId()).isEqualTo(700L);
        assertThat(dto.productId()).isEqualTo(10L);
        assertThat(dto.productName()).isEqualTo("Pao de queijo");
        assertThat(dto.consumerId()).isEqualTo(50L);
        assertThat(dto.quantity()).isEqualTo(3);
        assertThat(dto.unitPrice()).isEqualByComparingTo("5.20");
        assertThat(dto.totalPrice()).isEqualByComparingTo("15.60");
        assertThat(dto.status()).isEqualTo(OfferOrderStatus.RESERVED);
        assertThat(dto.createdAt()).isEqualTo(now);
    }

    @Test
    void toEntityMapsBasicFields() {
        OfferOrderDTO dto = new OfferOrderDTO(
                900L, 700L, 10L, "Pao de queijo", 50L, 2,
                new BigDecimal("5.20"), new BigDecimal("10.40"),
                OfferOrderStatus.RESERVED, LocalDateTime.now());

        OfferOrder entity = mapper.toEntity(dto);

        assertThat(entity).isNotNull();
        assertThat(entity.getId()).isEqualTo(900L);
        assertThat(entity.getQuantity()).isEqualTo(2);
        assertThat(entity.getStatus()).isEqualTo(OfferOrderStatus.RESERVED);
    }

    @Test
    void returnsNullWhenInputIsNull() {
        assertThat(mapper.toDto(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
    }
}
