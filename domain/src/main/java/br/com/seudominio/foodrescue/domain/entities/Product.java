package br.com.seudominio.foodrescue.domain.entities;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.builders.ProductBuilder;
import br.com.seudominio.foodrescue.domain.converter.MoneyConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;

import java.time.LocalDate;

/**
 * Product. This class represents a product sold by an {@link Establishment}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "products")
@SuppressWarnings("serial")
public class Product extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT")
    @SequenceGenerator(name = "SEQ_PRODUCT", sequenceName = "seq_product", allocationSize = 1)
    private Long id;

    /**
     * The establishment that owns this product.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private Establishment establishment;

    /**
     * Name of the product.
     */
    @NotBlank
    @Column(nullable = false)
    private String name;

    /**
     * Category of the product.
     */
    @NotBlank
    @Column(nullable = false)
    private String category;

    /**
     * Original (non-discounted) price of the product.
     */
    @NotNull
    @Convert(converter = MoneyConverter.class)
    @Column(name = "original_price", nullable = false)
    private Money originalPrice;

    /**
     * Current price of the product, which may reflect an accepted discount.
     */
    @Convert(converter = MoneyConverter.class)
    @Column(name = "current_price", nullable = false)
    private Money currentPrice;

    /**
     * Photo URL of the product.
     */
    @Column(name = "photo_url")
    private String photoUrl;

    /**
     * Current stock quantity of the product.
     */
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    /**
     * Expiration date of the product.
     */
    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    /**
     * Default constructor.
     */
    public Product() {
        super();
    }

    /**
     * Returns a new instance of the ProductBuilder for building Product objects.
     *
     * @return A new ProductBuilder instance.
     */
    public static ProductBuilder builder() {
        return new ProductBuilder();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Establishment getEstablishment() {
        return establishment;
    }

    public void setEstablishment(Establishment establishment) {
        this.establishment = establishment;
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
        return "Product [id=" + id + ", name=" + name + ", establishmentId="
                + (establishment == null ? null : establishment.getId()) + "]";
    }
}
