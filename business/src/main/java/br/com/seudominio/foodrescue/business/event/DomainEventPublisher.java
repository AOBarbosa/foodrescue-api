package br.com.seudominio.foodrescue.business.event;

/**
 * Publishes domain events so a use case can announce what happened without
 * knowing who reacts to it.
 *
 * <p>UC07 publishes {@link DiscountRecommendationRespondedEvent} instead of
 * calling the offer creation of UC08 directly; UC08 subscribes when it is
 * implemented. Neither side references the other.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
public interface DomainEventPublisher {

    /**
     * Publishes a domain event to whoever is listening.
     *
     * @param event the event to publish
     */
    void publish(Object event);
}
