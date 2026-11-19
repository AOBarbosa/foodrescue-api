package br.com.seudominio.perishrescue.domain.entities;

import br.com.seudominio.perishrescue.domain.builders.OfferOrderBuilder;
import br.com.seudominio.perishrescue.domain.enums.OfferOrderStatus;

import jakarta.persistence.Column;
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

/**
 * OfferOrder. This class represents a beneficiary's reservation/purchase of an
 * {@link Offer} (UC09).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "offer_orders")
@SuppressWarnings("serial")
public class OfferOrder extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OFFER_ORDER")
    @SequenceGenerator(name = "SEQ_OFFER_ORDER", sequenceName = "seq_offer_order", allocationSize = 1)
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
     * Lifecycle status of the order.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferOrderStatus status;

    /**
     * Default constructor.
     */
    public OfferOrder() {
        super();
    }

    /**
     * Returns a new instance of the OfferOrderBuilder for building OfferOrder objects.
     *
     * @return A new OfferOrderBuilder instance.
     */
    public static OfferOrderBuilder builder() {
        return new OfferOrderBuilder();
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

    public OfferOrderStatus getStatus() {
        return status;
    }

    public void setStatus(OfferOrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "OfferOrder [id=" + id + ", offerId=" + (offer == null ? null : offer.getId())
                + ", status=" + status + "]";
    }
}
