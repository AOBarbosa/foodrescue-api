package br.com.seudominio.perishrescue.domain.entities;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.builders.OfferBuilder;
import br.com.seudominio.perishrescue.domain.converter.MoneyConverter;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
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
import jakarta.persistence.Version;

import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * Offer. This class represents a surplus offer made from a {@link PerishableResource},
 * visible to beneficiaries (UC08/UC09). Carries an optimistic-locking
 * {@code version} column because concurrent reservations (UC09) must not
 * oversell the available quantity.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "offers")
@SuppressWarnings("serial")
public class Offer extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OFFER")
    @SequenceGenerator(name = "SEQ_OFFER", sequenceName = "seq_offer", allocationSize = 1)
    private Long id;

    /**
     * The resource this offer was made from.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private PerishableResource resource;

    /**
     * Discounted price offered to beneficiaries.
     */
    @Convert(converter = MoneyConverter.class)
    @Column(name = "discounted_price", nullable = false)
    private Money discountedPrice;

    /**
     * Quantity still available for reservation.
     */
    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    /**
     * Moment this offer expires.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Lifecycle status of the offer.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status;

    /**
     * Whether the offer is marked as priority/featured for beneficiaries.
     */
    @Column(nullable = false)
    private boolean priority;

    /**
     * Optimistic-locking version.
     */
    @Version
    @Column(nullable = false)
    private long version;

    /**
     * Default constructor.
     */
    public Offer() {
        super();
    }

    /**
     * Returns a new instance of the OfferBuilder for building Offer objects.
     *
     * @return A new OfferBuilder instance.
     */
    public static OfferBuilder builder() {
        return new OfferBuilder();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public PerishableResource getResource() {
        return resource;
    }

    public void setResource(PerishableResource resource) {
        this.resource = resource;
    }

    public Money getDiscountedPrice() {
        return discountedPrice;
    }

    public void setDiscountedPrice(Money discountedPrice) {
        this.discountedPrice = discountedPrice;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public OfferStatus getStatus() {
        return status;
    }

    /**
     * Sets the status directly. Meant for persistence, builders and tests;
     * business code moves the status with {@link #transitionTo}.
     *
     * @param status the status
     */
    public void setStatus(OfferStatus status) {
        this.status = status;
    }

    /**
     * Moves the offer to another state of the kernel's lifecycle state machine.
     *
     * @param target the new status
     * @throws br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException
     *         if the current status does not allow it
     */
    public void transitionTo(OfferStatus target) {
        this.status = LifecycleStateMachine.transition("offer", id, status, target);
    }

    /**
     * Takes units out of an available offer, moving it to
     * {@link OfferStatus#SOLD_OUT} when none is left.
     *
     * @param units how many units to take; positive and at most what is available
     * @throws br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException
     *         if the offer is not available or has fewer units left
     */
    public void takeUnits(int units) {
        if (status != OfferStatus.AVAILABLE) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d is not available: current status is %s", id, status));
        }
        if (units <= 0 || units > availableQuantity) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d has only %d units left", id, availableQuantity));
        }
        availableQuantity -= units;
        if (availableQuantity == 0) {
            transitionTo(OfferStatus.SOLD_OUT);
        }
    }

    /**
     * Gives back units of an expired or cancelled reservation. An open offer
     * gets them back (a sold-out one becomes available again); a closed offer
     * ignores them.
     *
     * @param units how many units to give back
     * @return {@code true} if the offer took the units back
     */
    public boolean returnUnits(int units) {
        if (status == null || !status.isOpen() || units <= 0) {
            return false;
        }
        availableQuantity += units;
        if (status == OfferStatus.SOLD_OUT) {
            transitionTo(OfferStatus.AVAILABLE);
        }
        return true;
    }

    /**
     * Marks the offer as priority/featured for beneficiaries (UC11).
     */
    public void markAsPriority() {
        this.priority = true;
    }
    public boolean isPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Offer [id=" + id + ", resourceId=" + (resource == null ? null : resource.getId())
                + ", status=" + status + "]";
    }
}
