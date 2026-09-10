package br.com.seudominio.foodrescue.domain.builders;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;

import java.time.LocalDateTime;

/**
 * Builder class for creating instances of {@link Sale}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class SaleBuilder {

    private Long id;
    private Product product;
    private int quantity;
    private Money unitPrice;
    private LocalDateTime soldAt;

    /**
     * Sets the ID of the sale.
     *
     * @param id the ID of the sale
     * @return the current instance of {@link SaleBuilder}
     */
    public SaleBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the sold product.
     *
     * @param product the sold product
     * @return the current instance of {@link SaleBuilder}
     */
    public SaleBuilder product(Product product) {
        this.product = product;
        return this;
    }

    /**
     * Sets the sold quantity.
     *
     * @param quantity the sold quantity
     * @return the current instance of {@link SaleBuilder}
     */
    public SaleBuilder quantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    /**
     * Sets the unit price practiced at the time of sale.
     *
     * @param unitPrice the unit price practiced at the time of sale
     * @return the current instance of {@link SaleBuilder}
     */
    public SaleBuilder unitPrice(Money unitPrice) {
        this.unitPrice = unitPrice;
        return this;
    }

    /**
     * Sets the moment the sale happened.
     *
     * @param soldAt the moment the sale happened
     * @return the current instance of {@link SaleBuilder}
     */
    public SaleBuilder soldAt(LocalDateTime soldAt) {
        this.soldAt = soldAt;
        return this;
    }

    /**
     * Builds and returns an instance of {@link Sale} with the set properties.
     *
     * @return a new instance of {@link Sale}
     */
    public Sale build() {
        Sale sale = new Sale();
        sale.setId(id);
        sale.setProduct(product);
        sale.setQuantity(quantity);
        sale.setUnitPrice(unitPrice);
        sale.setSoldAt(soldAt);
        return sale;
    }
}
