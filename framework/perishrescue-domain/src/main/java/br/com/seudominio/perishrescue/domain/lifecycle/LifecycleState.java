package br.com.seudominio.perishrescue.domain.lifecycle;

/**
 * A state of the PerishRescue lifecycle state machine.
 *
 * <p>Implemented by the status enums of the kernel ({@code OfferStatus},
 * {@code ReservationStatus}). Each state declares which states it may move
 * to; {@link LifecycleStateMachine} enforces it.</p>
 *
 * @param <S> the enum type of the states
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public interface LifecycleState<S extends Enum<S> & LifecycleState<S>> {

    /**
     * Whether this state may move to {@code target}.
     *
     * @param target the target state
     * @return {@code true} if the transition is allowed
     */
    boolean canTransitionTo(S target);

    /**
     * Whether no transition leaves this state.
     *
     * @return {@code true} for a final state
     */
    boolean isTerminal();
}
