package br.com.seudominio.perishrescue.domain.lifecycle;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.Reservation;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LifecycleStateMachineTest {

    private Offer offer(int units) {
        Offer offer = Offer.builder().discountedPrice(Money.of(5)).availableQuantity(units).build();
        offer.setId(700L);
        return offer;
    }

    @Test
    void anOpenOfferCanSellOutReopenExpireOrBeWithdrawn() {
        assertThat(OfferStatus.AVAILABLE.canTransitionTo(OfferStatus.SOLD_OUT)).isTrue();
        assertThat(OfferStatus.SOLD_OUT.canTransitionTo(OfferStatus.AVAILABLE)).isTrue();
        assertThat(OfferStatus.AVAILABLE.canTransitionTo(OfferStatus.EXPIRED)).isTrue();
        assertThat(OfferStatus.SOLD_OUT.canTransitionTo(OfferStatus.WITHDRAWN)).isTrue();
        assertThat(OfferStatus.AVAILABLE.canTransitionTo(OfferStatus.AVAILABLE)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OfferStatus.class, names = {"EXPIRED", "WITHDRAWN"})
    void aClosedOfferIsFinal(OfferStatus closed) {
        assertThat(closed.isTerminal()).isTrue();
        assertThat(closed.isOpen()).isFalse();
        for (OfferStatus target : OfferStatus.values()) {
            assertThat(closed.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    void aReservationOnlyLeavesReservedAndNeverComesBack() {
        assertThat(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.RESCUED)).isTrue();
        assertThat(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.EXPIRED)).isTrue();
        assertThat(ReservationStatus.RESERVED.canTransitionTo(ReservationStatus.CANCELLED)).isTrue();
        assertThat(ReservationStatus.RESCUED.canTransitionTo(ReservationStatus.CANCELLED)).isFalse();
        assertThat(ReservationStatus.CANCELLED.canTransitionTo(ReservationStatus.RESERVED)).isFalse();
    }

    @Test
    void entitiesRefuseATransitionTheStateMachineDoesNotAllow() {
        Offer offer = offer(3);
        offer.transitionTo(OfferStatus.EXPIRED);

        assertThatThrownBy(() -> offer.transitionTo(OfferStatus.AVAILABLE))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot move from EXPIRED to AVAILABLE");

        Reservation reservation = Reservation.builder().build();
        reservation.transitionTo(ReservationStatus.RESCUED);
        assertThatThrownBy(() -> reservation.transitionTo(ReservationStatus.EXPIRED))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void takingTheLastUnitsSellsTheOfferOutAndReturningThemReopensIt() {
        Offer offer = offer(2);

        offer.takeUnits(2);
        assertThat(offer.getAvailableQuantity()).isZero();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.SOLD_OUT);

        assertThat(offer.returnUnits(1)).isTrue();
        assertThat(offer.getAvailableQuantity()).isEqualTo(1);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.AVAILABLE);
    }

    @Test
    void aClosedOfferDoesNotTakeUnitsBack() {
        Offer offer = offer(2);
        offer.transitionTo(OfferStatus.WITHDRAWN);

        assertThat(offer.returnUnits(1)).isFalse();
        assertThat(offer.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void unitsCannotBeTakenFromASoldOutOfferOrBeyondWhatIsLeft() {
        Offer offer = offer(2);

        assertThatThrownBy(() -> offer.takeUnits(3)).hasMessageContaining("has only 2 units left");
        offer.takeUnits(2);
        assertThatThrownBy(() -> offer.takeUnits(1)).hasMessageContaining("is not available");
    }
}
