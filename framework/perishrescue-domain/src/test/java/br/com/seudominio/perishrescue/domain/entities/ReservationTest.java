package br.com.seudominio.perishrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

class ReservationTest {

    private Offer anyOffer() {
        Offerer offerer = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();

        PerishableResource resource = TestResource.builder()
                .offerer(offerer)
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .stockQuantity(10)
                .build();

        return Offer.builder()
                .resource(resource)
                .discountedPrice(Money.of(3.0))
                .availableQuantity(5)
                .expiresAt(LocalDateTime.now().plusHours(2))
                .build();
    }

    private Beneficiary anyBeneficiary() {
        return Beneficiary.builder()
                .name("Maria")
                .document("52998224725")
                .email("maria@example.com")
                .passwordHash("hashed-password")
                .build();
    }

    @Test
    void defaultsStatusToReserved() {
        Reservation reservation = Reservation.builder()
                .offer(anyOffer())
                .beneficiary(anyBeneficiary())
                .quantity(1)
                .build();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void buildsReservationWithAllFields() {
        Offer offer = anyOffer();
        Beneficiary beneficiary = anyBeneficiary();

        Reservation reservation = Reservation.builder()
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(2)
                .build();

        assertThat(reservation.getOffer()).isEqualTo(offer);
        assertThat(reservation.getBeneficiary()).isEqualTo(beneficiary);
        assertThat(reservation.getQuantity()).isEqualTo(2);
    }
}
