package br.com.seudominio.perishrescue.domain.entities;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.converter.MoneyConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * PerishableResource: the item an {@link Offerer} puts up for rescue.
 *
 * <p><b>Hot spot 1 of the PerishRescue framework.</b> The kernel keeps what
 * every perishable item has in common (owner, name, category, prices, stock,
 * declared expiration date) and drives catalog, stock and offers through this
 * type. Each instance <em>must</em> extend it with the attributes of its own
 * domain (a food item's allergens, a medicine's batch and ANVISA registry, a
 * flower's species) and with the rule that ends the item's useful life,
 * {@link #criticalDeadline(LocalDateTime)}.</p>
 *
 * <p>Mapped with {@link InheritanceType#JOINED}: the common columns live in
 * {@code resources}, and each subclass adds its own table whose primary key
 * is also a foreign key to {@code resources.id}.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "resources")
@Inheritance(strategy = InheritanceType.JOINED)
@SuppressWarnings("serial")
public abstract class PerishableResource extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_RESOURCE")
    @SequenceGenerator(name = "SEQ_RESOURCE", sequenceName = "seq_resource", allocationSize = 1)
    private Long id;

    /**
     * The offerer that owns this resource.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offerer_id", nullable = false)
    private Offerer offerer;

    /**
     * Name of the resource.
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * Category of the resource.
     */
    @NotBlank
    @Column(nullable = false)
    private String category;

    /**
     * Original (non-discounted) price of the resource.
     */
    @NotNull
    @Convert(converter = MoneyConverter.class)
    @Column(name = "original_price", nullable = false)
    private Money originalPrice;

    /**
     * Current price of the resource, which may reflect an accepted discount.
     */
    @Convert(converter = MoneyConverter.class)
    @Column(name = "current_price", nullable = false)
    private Money currentPrice;

    /**
     * Photo URL of the resource.
     */
    @Column(name = "photo_url")
    private String photoUrl;

    /**
     * Current stock quantity of the resource.
     */
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    /**
     * Expiration date of the resource.
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Constructor for subclasses.
     */
    protected PerishableResource() {
        super();
    }

    /**
     * The last moment this item can still be rescued, as seen at {@code now}.
     * The kernel uses it to monitor the item's lifecycle (it is the default
     * deadline of a new offer) without knowing what "perishable" means for
     * the instance: today's closing time for ready meals, the batch
     * expiration date for medicines, the end of a flower's aesthetic life.
     *
     * @param now the current date and time
     * @return the item's critical deadline
     */
    public abstract LocalDateTime criticalDeadline(LocalDateTime now);

    /**
     * Start of the item's perishability countdown. Defaults to the moment it
     * was registered; an instance overrides it when the countdown starts
     * elsewhere (a flower's harvest date, for instance).
     *
     * @return the start of the countdown
     */
    public LocalDateTime lifecycleStart() {
        return getCreationDate();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Offerer getOfferer() {
        return offerer;
    }

    public void setOfferer(Offerer offerer) {
        this.offerer = offerer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Money getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Money originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Money getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Money currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    @Override
    public String toString() {
        return "PerishableResource [id=" + id + ", name=" + name + ", offererId="
                + (offerer == null ? null : offerer.getId()) + "]";
    }
}
