package br.com.seudominio.perishrescue.business.scheduler;

import br.com.seudominio.perishrescue.business.services.OfferService;
import br.com.seudominio.perishrescue.business.services.ReservationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives the automatic transitions of the kernel's lifecycle state machine
 * (<b>frozen spot</b>): reservations whose pickup window passed become
 * {@code EXPIRED} and give their units back, then offers whose deadline passed
 * become {@code EXPIRED}. Runs every
 * {@code perishrescue.lifecycle.check-interval-ms} (default: one minute).
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class ExpirationMonitor {

    private static final Logger log = LoggerFactory.getLogger(ExpirationMonitor.class);

    private final ReservationService reservationService;
    private final OfferService offerService;

    /**
     * Constructor.
     *
     * @param reservationService the reservation service
     * @param offerService       the offer service
     */
    public ExpirationMonitor(ReservationService reservationService, OfferService offerService) {
        this.reservationService = reservationService;
        this.offerService = offerService;
    }

    /**
     * Expires overdue reservations first (so their units can still return to an
     * offer that is about to expire too), then overdue offers.
     */
    @Scheduled(fixedDelayString = "${perishrescue.lifecycle.check-interval-ms:60000}")
    public void expireOverdue() {
        int reservations = reservationService.expireOverdueReservations();
        int offers = offerService.expireOverdueOffers();
        if (reservations > 0 || offers > 0) {
            log.info("Lifecycle check: {} reservation(s) and {} offer(s) expired", reservations, offers);
        }
    }
}
