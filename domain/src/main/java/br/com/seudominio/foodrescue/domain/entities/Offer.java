package br.com.seudominio.foodrescue.domain.entities;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.builders.OfferBuilder;
import br.com.seudominio.foodrescue.domain.converter.MoneyConverter;
import br.com.seudominio.foodrescue.domain.enums.OfferStatus;

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
 * Offer. This class represents a surplus offer made from a {@link Product},
 * visible to consumers (UC08/UC09). Carries an optimistic-locking
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
     * The product this offer was made from.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Discounted price offered to consumers.
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
     * Whether the offer is marked as priority/featured for consumers.
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

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
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

    public void setStatus(OfferStatus status) {
        this.status = status;
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
        return "Offer [id=" + id + ", productId=" + (product == null ? null : product.getId())
                + ", status=" + status + "]";
    }
}
