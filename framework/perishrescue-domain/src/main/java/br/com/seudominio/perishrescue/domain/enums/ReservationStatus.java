package br.com.seudominio.perishrescue.domain.enums;

import br.com.seudominio.perishrescue.domain.entities.Reservation;
import br.com.seudominio.perishrescue.domain.lifecycle.LifecycleState;

/**
 * Lifecycle status of a {@link Reservation}. Part of the kernel's fixed state
 * machine:
 *
 * <pre>
 *   RESERVED -&gt; RESCUED    (the offerer confirmed the pickup)
 *   RESERVED -&gt; EXPIRED    (the pickup window passed; units go back to the offer)
 *   RESERVED -&gt; CANCELLED  (the beneficiary gave up; units go back to the offer)
 * </pre>
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public enum ReservationStatus implements LifecycleState<ReservationStatus> {

    /** Units held for the beneficiary, waiting for pickup. */
    RESERVED,

    /** Picked up. Final. */
    RESCUED,

    /** Not picked up within the pickup window. Final. */
    EXPIRED,

    /** Cancelled by the beneficiary. Final. */
    CANCELLED;

    @Override
    public boolean canTransitionTo(ReservationStatus target) {
        return this == RESERVED && target != RESERVED && target != null;
    }

    @Override
    public boolean isTerminal() {
        return this != RESERVED;
    }
}
