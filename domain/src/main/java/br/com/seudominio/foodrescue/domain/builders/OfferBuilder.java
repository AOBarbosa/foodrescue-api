package br.com.seudominio.foodrescue.domain.builders;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.OfferStatus;

import java.time.LocalDateTime;

/**
 * Builder class for creating instances of {@link Offer}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class OfferBuilder {

    private Long id;
    private Product product;
    private Money discountedPrice;
    private int availableQuantity;
    private LocalDateTime expiresAt;
    private OfferStatus status;
    private boolean priority;

    /**
     * Sets the ID of the offer.
     *
     * @param id the ID of the offer
     * @return the current instance of {@link OfferBuilder}
     */
    public OfferBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the product this offer is made from.
     *
     * @param product the product this offer is made from
     * @return the current instance of {@link OfferBuilder}
     */
    public OfferBuilder product(Product product) {
        this.product = product;
        return this;
    }

    /**
     * Sets the discounted price offered to consumers.
     *
     * @param discountedPrice the discounted price offered to consumers
     * @return the current instance of {@link OfferBuilder}
     */
    public OfferBuilder discountedPrice(Money discountedPrice) {
        this.discountedPrice = discountedPrice;
        return this;
    }

    /**
     * Sets the quantity available for reservation.
     *
     * @param availableQuantity the quantity available for reservation
     * @return the current instance of {@link OfferBuilder}
     */
    public OfferBuilder availableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
        return this;
    }

    /**
     * Sets the moment this offer expires.
     *
     * @param expiresAt the moment this offer expires
     * @return the current instance of {@link OfferBuilder}
     */
    public OfferBuilder expiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    /**
     * Sets the lifecycle status of the offer. Defaults to {@link OfferStatus#ACTIVE} when not set.
     *
     * @param status the lifecycle status of the offer
     * @return the current instance of {@link OfferBuilder}
     */
    public OfferBuilder status(OfferStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Sets whether the offer is marked as priority/featured for consumers.
     *
     * @param priority whether the offer is priority
     * @return the current instance of {@link OfferBuilder}
     */
    public OfferBuilder priority(boolean priority) {
        this.priority = priority;
        return this;
    }

    /**
     * Builds and returns an instance of {@link Offer} with the set properties.
     *
     * @return a new instance of {@link Offer}
     */
    public Offer build() {
        Offer offer = new Offer();
        offer.setId(id);
        offer.setProduct(product);
        offer.setDiscountedPrice(discountedPrice);
        offer.setAvailableQuantity(availableQuantity);
        offer.setExpiresAt(expiresAt);
        offer.setStatus(status != null ? status : OfferStatus.ACTIVE);
        offer.setPriority(priority);
        return offer;
    }
}
