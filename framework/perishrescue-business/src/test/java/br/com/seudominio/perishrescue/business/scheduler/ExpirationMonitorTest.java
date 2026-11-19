package br.com.seudominio.perishrescue.business.scheduler;

import br.com.seudominio.perishrescue.business.services.OfferService;
import br.com.seudominio.perishrescue.business.services.ReservationService;

import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpirationMonitorTest {

    @Test
    void aConflictOnOneReservationDoesNotStopTheOthersFromExpiring() {
        ReservationService reservations = mock(ReservationService.class);
        OfferService offers = mock(OfferService.class);
        when(reservations.findOverdueReservationIds()).thenReturn(List.of(1L, 2L, 3L));
        when(reservations.expireReservation(2L))
                .thenThrow(new ObjectOptimisticLockingFailureException("Offer", 700L));
        when(reservations.expireReservation(1L)).thenReturn(true);
        when(reservations.expireReservation(3L)).thenReturn(true);
        when(offers.findOverdueOfferIds()).thenReturn(List.of(700L));

        new ExpirationMonitor(reservations, offers).expireOverdue();

        verify(reservations).expireReservation(1L);
        verify(reservations).expireReservation(3L);
        verify(offers).expireOffer(700L);
    }
}
