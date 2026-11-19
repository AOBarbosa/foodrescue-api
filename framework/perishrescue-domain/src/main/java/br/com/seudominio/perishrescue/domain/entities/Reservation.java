package br.com.seudominio.perishrescue.domain.entities;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.builders.ReservationBuilder;
import br.com.seudominio.perishrescue.domain.converter.MoneyConverter;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;
import br.com.seudominio.perishrescue.domain.lifecycle.LifecycleStateMachine;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * Reservation. A beneficiary's hold on units of an {@link Offer}, waiting to be
 * picked up at the offerer (UC09).
 *
 * <p>The unit price is frozen when the reservation is made, so later
 * repricing of the offer does not change what the beneficiary pays. The
 * beneficiary must pick the units up, showing {@code pickupCode}, before
 * {@code pickupDeadline}; otherwise the kernel expires the reservation and
 * returns the units to the offer.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "reservations")
@SuppressWarnings("serial")
public class Reservation extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RESERVATION")
    @SequenceGenerator(name = "SEQ_RESERVATION", sequenceName = "seq_reservation", allocationSize = 1)
    private Long id;

    /**
     * The reserved offer.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    /**
     * The beneficiary that made the reservation.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id", nullable = false)
    private Beneficiary beneficiary;

    /**
     * Reserved quantity.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Price of one unit, frozen at reservation time.
     */
    @Convert(converter = MoneyConverter.class)
    @Column(name = "unit_price", nullable = false)
    private Money unitPrice;

    /**
     * Last moment the units can be picked up.
     */
    @Column(name = "pickup_deadline", nullable = false)
    private LocalDateTime pickupDeadline;

    /**
     * Code the beneficiary shows at pickup; the offerer types it to confirm.
     */
    @Column(name = "pickup_code", nullable = false, length = 12)
    private String pickupCode;

    /**
     * Moment the pickup was confirmed, when {@link ReservationStatus#RESCUED}.
     */
    @Column(name = "rescued_at")
    private LocalDateTime rescuedAt;

    /**
     * Lifecycle status of the reservation.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    /**
     * Default constructor.
     */
    public Reservation() {
        super();
    }

    /**
     * Returns a new instance of the ReservationBuilder for building Reservation objects.
     *
     * @return A new ReservationBuilder instance.
     */
    public static ReservationBuilder builder() {
        return new ReservationBuilder();
    }

    /**
     * Moves the reservation to another state of the kernel's lifecycle state
     * machine.
     *
     * @param target the new status
     * @throws br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException
     *         if the current status does not allow it
     */
    public void transitionTo(ReservationStatus target) {
        this.status = LifecycleStateMachine.transition("reservation", id, status, target);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Offer getOffer() {
        return offer;
    }

    public void setOffer(Offer offer) {
        this.offer = offer;
    }

    public Beneficiary getBeneficiary() {
        return beneficiary;
    }

    public void setBeneficiary(Beneficiary beneficiary) {
        this.beneficiary = beneficiary;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Money unitPrice) {
        this.unitPrice = unitPrice;
    }

    public LocalDateTime getPickupDeadline() {
        return pickupDeadline;
    }

    public void setPickupDeadline(LocalDateTime pickupDeadline) {
        this.pickupDeadline = pickupDeadline;
    }

    public String getPickupCode() {
        return pickupCode;
    }

    public void setPickupCode(String pickupCode) {
        this.pickupCode = pickupCode;
    }

    public LocalDateTime getRescuedAt() {
        return rescuedAt;
    }

    public void setRescuedAt(LocalDateTime rescuedAt) {
        this.rescuedAt = rescuedAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    /**
     * Sets the status directly. Meant for persistence, builders and tests;
     * business code moves the status with {@link #transitionTo}.
     *
     * @param status the status
     */
    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Reservation [id=" + id + ", offerId=" + (offer == null ? null : offer.getId())
                + ", status=" + status + "]";
    }
}
