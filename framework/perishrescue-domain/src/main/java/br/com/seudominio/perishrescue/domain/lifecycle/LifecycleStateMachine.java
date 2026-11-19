package br.com.seudominio.perishrescue.domain.lifecycle;

import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;

/**
 * The fixed lifecycle state machine of the PerishRescue kernel
 * (<b>frozen spot</b>): offers move between {@code AVAILABLE}, {@code SOLD_OUT},
 * {@code EXPIRED} and {@code WITHDRAWN}; reservations from {@code RESERVED} to
 * {@code RESCUED}, {@code EXPIRED} or {@code CANCELLED}.
 *
 * <p>Entities never assign their status directly in business code: they call
 * {@link #transition}, which refuses any move the current state does not
 * allow. Instances cannot add states or transitions; they react to them
 * through the domain events the kernel publishes on each transition.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public final class LifecycleStateMachine {

    private LifecycleStateMachine() {
    }

    /**
     * Validates a transition and returns the target state.
     *
     * @param entity  the entity name, for the error message (e.g. "offer")
     * @param id      the entity id, for the error message
     * @param current the current state
     * @param target  the requested state
     * @param <S>     the state type
     * @return {@code target}
     * @throws BusinessRuleViolationException if the transition is not allowed
     */
    public static <S extends Enum<S> & LifecycleState<S>> S transition(String entity, Long id, S current, S target) {
        if (current == null || !current.canTransitionTo(target)) {
            throw new BusinessRuleViolationException(String.format(
                    "%s %s cannot move from %s to %s", entity, id, current, target));
        }
        return target;
    }
}
