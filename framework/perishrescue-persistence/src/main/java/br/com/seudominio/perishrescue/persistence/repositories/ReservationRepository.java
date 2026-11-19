package br.com.seudominio.perishrescue.persistence.repositories;

import br.com.seudominio.perishrescue.domain.entities.Reservation;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Reservation repository - database operations of the {@link Reservation} entity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Repository
public interface ReservationRepository extends GenericRepository<Reservation> {

    /**
     * Finds every active reservation made by a beneficiary, newest first (UC09).
     *
     * @param beneficiaryId the beneficiary identifier
     * @return the beneficiary's reservations
     */
    List<Reservation> findByBeneficiaryIdAndActiveTrueOrderByCreationDateDesc(Long beneficiaryId);

    /**
     * Finds every active reservation of an offerer's offers, newest first.
     *
     * @param offererId the offerer identifier
     * @return the reservations made on the offerer's offers
     */
    List<Reservation> findByOfferResourceOffererIdAndActiveTrueOrderByCreationDateDesc(Long offererId);

    /**
     * Units a beneficiary reserved in a period, among reservations in the given statuses.
     *
     * @param beneficiaryId the beneficiary identifier
     * @param from          start of the period, inclusive
     * @param to            end of the period, exclusive
     * @param statuses      the statuses that count
     * @return the reserved units, {@code 0} if none
     */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM Reservation r "
            + "WHERE r.beneficiary.id = :beneficiaryId AND r.active = true "
            + "AND r.creationDate >= :from AND r.creationDate < :to AND r.status IN :statuses")
    long sumQuantityByBeneficiaryInPeriod(@Param("beneficiaryId") Long beneficiaryId,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to,
                                         @Param("statuses") Collection<ReservationStatus> statuses);

    /**
     * Finds reservations in a status whose pickup window ended at or before {@code now}.
     *
     * @param status the status (the kernel looks for {@code RESERVED})
     * @param now    the current date and time
     * @return the overdue reservations
     */
    List<Reservation> findByStatusAndPickupDeadlineLessThanEqualAndActiveTrue(ReservationStatus status,
                                                                             LocalDateTime now);
}
