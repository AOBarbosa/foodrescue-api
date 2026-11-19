package br.com.seudominio.perishrescue.business.rescue;

import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;
import br.com.seudominio.perishrescue.persistence.repositories.ReservationRepository;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * The kernel's {@link ReservationHistory}, backed by the reservation repository.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class RepositoryReservationHistory implements ReservationHistory {

    private final ReservationRepository reservationRepository;

    /**
     * Constructor.
     *
     * @param reservationRepository the reservation repository
     */
    public RepositoryReservationHistory(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Override
    public int unitsReservedBy(Long beneficiaryId, LocalDateTime from, LocalDateTime to) {
        return Math.toIntExact(reservationRepository.sumQuantityByBeneficiaryInPeriod(
                beneficiaryId, from, to, EnumSet.of(ReservationStatus.RESERVED, ReservationStatus.RESCUED)));
    }
}
