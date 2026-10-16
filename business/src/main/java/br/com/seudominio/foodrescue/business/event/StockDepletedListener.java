package br.com.seudominio.foodrescue.business.event;

import br.com.seudominio.foodrescue.business.services.OfferService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Closes the open offers of a product that ran out of stock (UC08).
 *
 * <p>Enforces the acceptance criterion <em>"an offer cannot stay active with
 * zero stock"</em> from the side the product controls. {@code ProductService}
 * only announces that the stock reached zero &mdash; it does not know that
 * offers exist.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Component
public class StockDepletedListener {

    private static final Logger log = LoggerFactory.getLogger(StockDepletedListener.class);

    private final OfferService offerService;

    /**
     * Constructor.
     *
     * @param offerService the offer service
     */
    public StockDepletedListener(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * Closes whatever is still open for the depleted product.
     *
     * <p>Runs only after the transaction that emptied the stock commits, so a
     * failure here never rolls back the sale or the inventory update that
     * caused it.</p>
     *
     * @param event the depleted product
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductStockDepleted(ProductStockDepletedEvent event) {
        try {
            int closed = offerService.closeOffersForDepletedProduct(event.productId());
            if (closed > 0) {
                log.info("Closed {} offer(s) of product {}: stock reached zero", closed, event.productId());
            }
        } catch (RuntimeException e) {
            log.error("Failed to close the offers of product {} after its stock reached zero",
                    event.productId(), e);
        }
    }
}
