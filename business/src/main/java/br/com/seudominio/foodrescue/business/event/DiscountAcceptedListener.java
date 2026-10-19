package br.com.seudominio.foodrescue.business.event;

import br.com.seudominio.foodrescue.business.services.OfferService;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes an offer when an establishment accepts a discount recommendation (UC08).
 *
 * <p>This is the only link between UC07 and UC08, and it points one way:
 * {@code AiRecommendationService} publishes the event and never knows this
 * class exists. Another reaction to the same event can be added without
 * touching UC07.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Component
public class DiscountAcceptedListener {

    private static final Logger log = LoggerFactory.getLogger(DiscountAcceptedListener.class);

    private final OfferService offerService;

    /**
     * Constructor.
     *
     * @param offerService the offer service
     */
    public DiscountAcceptedListener(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * Reacts to an answered discount recommendation, publishing an offer when
     * the answer actually changed the price.
     *
     * <p>Runs only after the answering transaction commits: a failure here must
     * never roll back the discount the establishment already accepted. The
     * exception is caught and logged for the same reason.</p>
     *
     * @param event the answered recommendation
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDiscountRecommendationResponded(DiscountRecommendationRespondedEvent event) {
        if (!priceWasChanged(event.status())) {
            return;
        }

        try {
            offerService.createFromAcceptedDiscount(event.productId(), event.establishmentId())
                    .ifPresentOrElse(
                            offer -> log.info("Offer {} published for product {} after recommendation {} was {}",
                                    offer.id(), event.productId(), event.recommendationId(), event.status()),
                            () -> log.info("No offer published for product {}: no stock left", event.productId()));
        } catch (RuntimeException e) {
            log.error("Failed to publish an offer for product {} after recommendation {} was {}",
                    event.productId(), event.recommendationId(), event.status(), e);
        }
    }

    private boolean priceWasChanged(RecommendationStatus status) {
        return status == RecommendationStatus.ACCEPTED || status == RecommendationStatus.ADJUSTED;
    }
}
