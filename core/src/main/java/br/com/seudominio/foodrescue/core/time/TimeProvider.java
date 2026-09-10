package br.com.seudominio.foodrescue.core.time;

import java.time.LocalDateTime;

/**
 * Abstraction for obtaining the current date/time, injectable into upper layers.
 * Avoids {@code LocalDateTime.now()} scattered across the codebase and allows
 * time-based rules (e.g. expiration in the past) to be tested deterministically.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public interface TimeProvider {

    /**
     * Returns the current date/time.
     *
     * @return the current date/time
     */
    LocalDateTime now();
}
