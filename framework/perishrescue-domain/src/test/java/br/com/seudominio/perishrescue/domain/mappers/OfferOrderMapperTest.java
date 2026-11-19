package br.com.seudominio.perishrescue.domain.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.OfferOrder;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.OfferOrderStatus;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class OfferOrderMapperTest {

    private final OfferOrderMapper mapper = new OfferOrderMapper();

    @Test
    void toDtoMapsAllFieldsAndComputesTotalPrice() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 14, 15, 0);

        PerishableResource resource = TestResource.builder()
                .id(10L)
                .name("Pao de queijo")
                .build();

        Offer offer = Offer.builder()
                .id(700L)
                .resource(resource)
                .discountedPrice(Money.of(new BigDecimal("5.20")))
                .availableQuantity(5)
                .expiresAt(now.plusHours(3))
                .build();

        Beneficiary beneficiary = Beneficiary.builder()
                .id(50L)
                .name("Maria")
                .document("52998224725")
                .build();

        OfferOrder order = OfferOrder.builder()
                .id(900L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(3)
                .status(OfferOrderStatus.RESERVED)
                .build();
        order.setCreationDate(now);

        OfferOrderDTO dto = mapper.toDto(order);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(900L);
        assertThat(dto.offerId()).isEqualTo(700L);
        assertThat(dto.resourceId()).isEqualTo(10L);
        assertThat(dto.resourceName()).isEqualTo("Pao de queijo");
        assertThat(dto.beneficiaryId()).isEqualTo(50L);
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
