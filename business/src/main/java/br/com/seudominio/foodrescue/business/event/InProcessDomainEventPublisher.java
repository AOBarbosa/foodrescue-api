package br.com.seudominio.foodrescue.business.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * {@link DomainEventPublisher} backed by Spring's own event mechanism, so
 * subscribers are plain {@code @EventListener} methods inside the same
 * application.
 *
 * <p>The indirection exists so use cases depend on the {@code business}
 * abstraction rather than on {@link ApplicationEventPublisher}: moving to an
 * out-of-process broker later would replace this class alone.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Component
public class InProcessDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Constructor.
     *
     * @param applicationEventPublisher the Spring event publisher
     */
    public InProcessDomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publish(Object event) {
        applicationEventPublisher.publishEvent(event);
    }
}
