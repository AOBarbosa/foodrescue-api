package br.com.seudominio.foodrescue.domain.entities;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.builders.SaleBuilder;
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

import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * Sale. This class represents a recorded over-the-counter sale of a
 * {@link Product}, feeding the sales history later used by demand
 * forecasting (UC05).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "sales")
@SuppressWarnings("serial")
public class Sale extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SALE")
    @SequenceGenerator(name = "SEQ_SALE", sequenceName = "seq_sale", allocationSize = 1)
    private Long id;

    /**
     * The sold product.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Sold quantity.
     */
    @Column(nullable = false)
    private int quantity;

    /**
     * Unit price practiced at the time of sale.
     */
    @Convert(converter = MoneyConverter.class)
    @Column(name = "unit_price", nullable = false)
    private Money unitPrice;

    /**
     * Moment the sale happened.
     */
    @Column(name = "sold_at", nullable = false)
    private LocalDateTime soldAt;

    /**
     * Default constructor.
     */
    public Sale() {
        super();
    }

    /**
     * Returns a new instance of the SaleBuilder for building Sale objects.
     *
     * @return A new SaleBuilder instance.
     */
    public static SaleBuilder builder() {
        return new SaleBuilder();
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

    public LocalDateTime getSoldAt() {
        return soldAt;
    }

    public void setSoldAt(LocalDateTime soldAt) {
        this.soldAt = soldAt;
    }

    @Override
    public String toString() {
        return "Sale [id=" + id + ", productId=" + (product == null ? null : product.getId())
                + ", quantity=" + quantity + "]";
    }
}
