package br.com.seudominio.foodrescue.domain.builders;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Product;

import java.time.LocalDate;

/**
 * Builder class for creating instances of {@link Product}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class ProductBuilder {

    private Long id;
    private Establishment establishment;
    private String name;
    private String category;
    private Money originalPrice;
    private Money currentPrice;
    private String photoUrl;
    private int stockQuantity;
    private LocalDate expirationDate;

    /**
     * Sets the ID of the product.
     *
     * @param id the ID of the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the establishment that owns the product.
     *
     * @param establishment the establishment that owns the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder establishment(Establishment establishment) {
        this.establishment = establishment;
        return this;
    }

    /**
     * Sets the name of the product.
     *
     * @param name the name of the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the category of the product.
     *
     * @param category the category of the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder category(String category) {
        this.category = category;
        return this;
    }

    /**
     * Sets the original (non-discounted) price of the product.
     *
     * @param originalPrice the original price of the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder originalPrice(Money originalPrice) {
        this.originalPrice = originalPrice;
        return this;
    }

    /**
     * Sets the current price of the product.
     *
     * @param currentPrice the current price of the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder currentPrice(Money currentPrice) {
        this.currentPrice = currentPrice;
        return this;
    }

    /**
     * Sets the photo URL of the product.
     *
     * @param photoUrl the photo URL of the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder photoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
        return this;
    }

    /**
     * Sets the stock quantity of the product.
     *
     * @param stockQuantity the stock quantity of the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder stockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
        return this;
    }

    /**
     * Sets the expiration date of the product.
     *
     * @param expirationDate the expiration date of the product
     * @return the current instance of {@link ProductBuilder}
     */
    public ProductBuilder expirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    /**
     * Builds and returns an instance of {@link Product} with the set properties.
     * When {@code currentPrice} isn't set explicitly, it defaults to {@code originalPrice}.
     *
     * @return a new instance of {@link Product}
     */
    public Product build() {
        Product product = new Product();
        product.setId(id);
        product.setEstablishment(establishment);
        product.setName(name);
        product.setCategory(category);
        product.setOriginalPrice(originalPrice);
        product.setCurrentPrice(currentPrice != null ? currentPrice : originalPrice);
        product.setPhotoUrl(photoUrl);
        product.setStockQuantity(stockQuantity);
        product.setExpirationDate(expirationDate);
        return product;
    }
}
