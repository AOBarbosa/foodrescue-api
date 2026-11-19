package br.com.seudominio.perishrescue.domain.testsupport;

import br.com.seudominio.perishrescue.domain.builders.ResourceBuilder;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Minimal concrete {@link PerishableResource} used by the kernel's own tests,
 * so the framework is tested without depending on any real instance.
 *
 * <p>Its critical deadline is the end of its expiration date, or the end of
 * the current day when none is declared.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "test_resources")
@SuppressWarnings("serial")
public class TestResource extends PerishableResource {

    /**
     * Default constructor.
     */
    public TestResource() {
        super();
    }

    /**
     * Returns a builder for {@code TestResource}.
     *
     * @return a new builder
     */
    public static ResourceBuilder<TestResource> builder() {
        return new ResourceBuilder<>(TestResource::new);
    }

    @Override
    public LocalDateTime criticalDeadline(LocalDateTime now) {
        return getExpirationDate() != null
                ? getExpirationDate().atTime(LocalTime.MAX)
                : now.toLocalDate().atTime(LocalTime.MAX);
    }
}
