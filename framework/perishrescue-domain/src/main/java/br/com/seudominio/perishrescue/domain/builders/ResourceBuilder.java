package br.com.seudominio.perishrescue.domain.builders;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import java.time.LocalDate;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Builder for the common attributes of any {@link PerishableResource}.
 *
 * <p>{@code PerishableResource} is abstract, so the builder is created with
 * the factory of the concrete type (e.g. {@code new ResourceBuilder<>(FoodItem::new)})
 * and returns that type from {@link #build()}; instance-specific attributes are
 * set on the built object.</p>
 *
 * @param <R> the concrete resource type
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class ResourceBuilder<R extends PerishableResource> {

    private final Supplier<R> factory;

    private Long id;
    private Offerer offerer;
    private String name;
    private String category;
    private Money originalPrice;
    private Money currentPrice;
    private String photoUrl;
    private int stockQuantity;
    private LocalDate expirationDate;

    /**
     * Creates a builder for the concrete resource type made by {@code factory}.
     *
     * @param factory creates an empty instance of the concrete type
     */
    public ResourceBuilder(Supplier<R> factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    /**
     * Sets the ID of the resource.
     *
     * @param id the ID of the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the offerer that owns the resource.
     *
     * @param offerer the offerer that owns the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> offerer(Offerer offerer) {
        this.offerer = offerer;
        return this;
    }

    /**
     * Sets the name of the resource.
     *
     * @param name the name of the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> name(String name) {
        this.name = name;
        return this;
    }

    /**
     * Sets the category of the resource.
     *
     * @param category the category of the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> category(String category) {
        this.category = category;
        return this;
    }

    /**
     * Sets the original (non-discounted) price of the resource.
     *
     * @param originalPrice the original price of the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> originalPrice(Money originalPrice) {
        this.originalPrice = originalPrice;
        return this;
    }

    /**
     * Sets the current price of the resource.
     *
     * @param currentPrice the current price of the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> currentPrice(Money currentPrice) {
        this.currentPrice = currentPrice;
        return this;
    }

    /**
     * Sets the photo URL of the resource.
     *
     * @param photoUrl the photo URL of the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> photoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
        return this;
    }

    /**
     * Sets the stock quantity of the resource.
     *
     * @param stockQuantity the stock quantity of the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> stockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
        return this;
    }

    /**
     * Sets the expiration date of the resource.
     *
     * @param expirationDate the expiration date of the resource
     * @return the current instance of {@link ResourceBuilder}
     */
    public ResourceBuilder<R> expirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    /**
     * Builds and returns an instance of {@link PerishableResource} with the set properties.
     * When {@code currentPrice} isn't set explicitly, it defaults to {@code originalPrice}.
     *
     * @return a new instance of {@link PerishableResource}
     */
    public R build() {
        R resource = factory.get();
        resource.setId(id);
        resource.setOfferer(offerer);
        resource.setName(name);
        resource.setCategory(category);
        resource.setOriginalPrice(originalPrice);
        resource.setCurrentPrice(currentPrice != null ? currentPrice : originalPrice);
        resource.setPhotoUrl(photoUrl);
        resource.setStockQuantity(stockQuantity);
        resource.setExpirationDate(expirationDate);
        return resource;
    }
}
