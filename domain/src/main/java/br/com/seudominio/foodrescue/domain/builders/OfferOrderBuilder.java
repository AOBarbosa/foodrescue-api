package br.com.seudominio.foodrescue.domain.builders;

import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.OfferOrder;
import br.com.seudominio.foodrescue.domain.enums.OfferOrderStatus;

/**
 * Builder class for creating instances of {@link OfferOrder}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class OfferOrderBuilder {

    private Long id;
    private Offer offer;
    private Consumer consumer;
    private int quantity;
    private OfferOrderStatus status;

    /**
     * Sets the ID of the offer order.
     *
     * @param id the ID of the offer order
     * @return the current instance of {@link OfferOrderBuilder}
     */
    public OfferOrderBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the reserved offer.
     *
     * @param offer the reserved offer
     * @return the current instance of {@link OfferOrderBuilder}
     */
    public OfferOrderBuilder offer(Offer offer) {
        this.offer = offer;
        return this;
    }

    /**
     * Sets the consumer that made the reservation.
     *
     * @param consumer the consumer that made the reservation
     * @return the current instance of {@link OfferOrderBuilder}
     */
    public OfferOrderBuilder consumer(Consumer consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * Sets the reserved quantity.
     *
     * @param quantity the reserved quantity
     * @return the current instance of {@link OfferOrderBuilder}
     */
    public OfferOrderBuilder quantity(int quantity) {
        this.quantity = quantity;
        return this;
    }

    /**
     * Sets the lifecycle status of the order. Defaults to {@link OfferOrderStatus#RESERVED} when not set.
     *
     * @param status the lifecycle status of the order
     * @return the current instance of {@link OfferOrderBuilder}
     */
    public OfferOrderBuilder status(OfferOrderStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Builds and returns an instance of {@link OfferOrder} with the set properties.
     *
     * @return a new instance of {@link OfferOrder}
     */
    public OfferOrder build() {
        OfferOrder offerOrder = new OfferOrder();
        offerOrder.setId(id);
        offerOrder.setOffer(offer);
        offerOrder.setConsumer(consumer);
        offerOrder.setQuantity(quantity);
        offerOrder.setStatus(status != null ? status : OfferOrderStatus.RESERVED);
        return offerOrder;
    }
}
