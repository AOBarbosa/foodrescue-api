package br.com.seudominio.perishrescue.business.event;

import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;

/**
 * Published by the kernel when a reservation is created ({@code from} is
 * {@code null}) and whenever it moves in the lifecycle state machine
 * (rescued, expired, cancelled). Instances react to it without changing the
 * kernel's flow.
 *
 * @param reservationId the reservation
 * @param offerId       the reserved offer
 * @param beneficiaryId the beneficiary
 * @param from          the previous status, {@code null} on creation
 * @param to            the new status
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public record ReservationStatusChangedEvent(
        Long reservationId, Long offerId, Long beneficiaryId, ReservationStatus from, ReservationStatus to) {
}
