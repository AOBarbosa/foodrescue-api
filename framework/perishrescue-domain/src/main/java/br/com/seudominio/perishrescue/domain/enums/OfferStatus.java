package br.com.seudominio.perishrescue.domain.enums;

import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.lifecycle.LifecycleState;

import java.util.EnumSet;
import java.util.Set;

/**
 * Lifecycle status of an {@link Offer} (a batch of a resource put up for
 * rescue). Part of the kernel's fixed state machine:
 *
 * <pre>
 *   AVAILABLE &lt;-&gt; SOLD_OUT      (units reserved / returned by an expired or cancelled reservation)
 *   AVAILABLE | SOLD_OUT -&gt; EXPIRED    (the offer's deadline passed)
 *   AVAILABLE | SOLD_OUT -&gt; WITHDRAWN  (closed by the offerer, superseded, or the stock ran out)
 * </pre>
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public enum OfferStatus implements LifecycleState<OfferStatus> {

    /** Open, with units left to reserve. */
    AVAILABLE,

    /** Open, but every unit is currently reserved. */
    SOLD_OUT,

    /** Its deadline passed. Final. */
    EXPIRED,

    /** Closed before its deadline. Final. */
    WITHDRAWN;

    @Override
    public boolean canTransitionTo(OfferStatus target) {
        return next().contains(target);
    }

    @Override
    public boolean isTerminal() {
        return next().isEmpty();
    }

    /**
     * Whether the offer is still running (available or sold out).
     *
     * @return {@code true} for {@link #AVAILABLE} and {@link #SOLD_OUT}
     */
    public boolean isOpen() {
        return this == AVAILABLE || this == SOLD_OUT;
    }

    /**
     * The open statuses, for repository queries.
     *
     * @return {@link #AVAILABLE} and {@link #SOLD_OUT}
     */
    public static Set<OfferStatus> openStatuses() {
        return EnumSet.of(AVAILABLE, SOLD_OUT);
    }

    private Set<OfferStatus> next() {
        return switch (this) {
            case AVAILABLE -> EnumSet.of(SOLD_OUT, EXPIRED, WITHDRAWN);
            case SOLD_OUT -> EnumSet.of(AVAILABLE, EXPIRED, WITHDRAWN);
            case EXPIRED, WITHDRAWN -> EnumSet.noneOf(OfferStatus.class);
        };
    }
}
