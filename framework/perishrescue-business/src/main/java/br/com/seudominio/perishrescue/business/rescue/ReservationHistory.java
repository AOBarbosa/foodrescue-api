package br.com.seudominio.perishrescue.business.rescue;

import java.time.LocalDateTime;

/**
 * Read-only view of past reservations, for {@link RescueRule}s that limit how
 * much a beneficiary may rescue (e.g. a daily quota per CPF). Rules query it;
 * they never touch the repositories directly.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public interface ReservationHistory {

    /**
     * Units a beneficiary reserved in a period, counting reservations still
     * held or already picked up (cancelled and expired ones do not count).
     *
     * @param beneficiaryId the beneficiary
     * @param from          the start of the period, inclusive
     * @param to            the end of the period, exclusive
     * @return the reserved units
     */
    int unitsReservedBy(Long beneficiaryId, LocalDateTime from, LocalDateTime to);
}
