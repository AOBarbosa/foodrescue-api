package br.com.seudominio.foodrescue.core.time;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Default {@link TimeProvider} implementation, backed by the system clock.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
@Component
public class SystemTimeProvider implements TimeProvider {

    /**
     * Returns the current date/time from the system clock.
     *
     * @return the current date/time
     */
    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
