package br.com.seudominio.perishrescue.domain.builders;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.Reservation;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;

import java.time.LocalDateTime;

/**
 * Builder class for creating instances of {@link Reservation}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class ReservationBuilder {

    private Long id;
    private Offer offer;
    private Beneficiary beneficiary;
    private int quantity;
    private Money unitPrice;
    private LocalDateTime pickupDeadline;
    private String pickupCode;
    private ReservationStatus status;

    /**
     * Sets the ID of the offer order.
     *
     * @param id the ID of the offer order
     * @return the current instance of {@link ReservationBuilder}
     */
    public ReservationBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the reserved offer.
     *
     * @param offer the reserved offer
     * @return the current instance of {@link ReservationBuilder}
     */
    public ReservationBuilder offer(Offer offer) {
        this.offer = offer;
        return this;
    }

    /**
     * Sets the beneficiary that made the reservation.
     *
     * @param beneficiary the beneficiary that made the reservation
     * @return the current instance of {@link ReservationBuilder}
     */
    public ReservationBuilder beneficiary(Beneficiary beneficiary) {
        this.beneficiary = beneficiary;
        return this;
    }

    /**
     * Sets the reserved quantity.
     *
     * @param quantity the reserved quantity
     * @return the current instance of {@link ReservationBuilder}
     */
    public ReservationBuilder quantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    /**
     * Sets the lifecycle status of the order. Defaults to {@link ReservationStatus#RESERVED} when not set.
     *
     * @param status the lifecycle status of the order
     * @return the current instance of {@link ReservationBuilder}
     */
    public ReservationBuilder unitPrice(Money unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    public ReservationBuilder pickupDeadline(LocalDateTime pickupDeadline) {
        this.pickupDeadline = pickupDeadline;
        return this;
    }

    public ReservationBuilder pickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
        return this;
    }

    public ReservationBuilder status(ReservationStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Builds and returns an instance of {@link Reservation} with the set properties.
     *
     * @return a new instance of {@link Reservation}
     */
    public Reservation build() {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setOffer(offer);
        reservation.setBeneficiary(beneficiary);
        reservation.setQuantity(quantity);
        reservation.setUnitPrice(unitPrice);
        reservation.setPickupDeadline(pickupDeadline);
        reservation.setPickupCode(pickupCode);
        reservation.setStatus(status != null ? status : ReservationStatus.RESERVED);
        return reservation;
    }
}
