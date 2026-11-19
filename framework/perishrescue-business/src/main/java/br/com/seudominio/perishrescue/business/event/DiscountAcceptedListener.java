package br.com.seudominio.perishrescue.business.event;

import br.com.seudominio.perishrescue.business.services.OfferService;
import br.com.seudominio.perishrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes an offer when an offerer accepts a discount recommendation (UC08).
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
     * never roll back the discount the offerer already accepted. The
     * exception is caught and logged for the same reason.</p>
     *
     * @param event the answered recommendation
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDiscountRecommendationResponded(DiscountRecommendationRespondedEvent event) {
        if (event.status() != RecommendationStatus.ACCEPTED && event.status() != RecommendationStatus.ADJUSTED) {
            return;
        }

        try {
            if (event.recommendationType() == RecommendationType.SURPLUS_DESTINATION) {
                offerService.markOrPublishPriorityOfferForResource(event.resourceId(), event.offererId())
                        .ifPresentOrElse(
                                offer -> log.info("Offer {} marked as priority for resource {} after recommendation {} was accepted",
                                        offer.id(), event.resourceId(), event.recommendationId()),
                                () -> log.info("No priority offer for resource {}: no stock left", event.resourceId()));
            } else {
                offerService.createFromAcceptedDiscount(event.resourceId(), event.offererId())
                        .ifPresentOrElse(
                                offer -> log.info("Offer {} published for resource {} after recommendation {} was {}",
                                        offer.id(), event.resourceId(), event.recommendationId(), event.status()),
                                () -> log.info("No offer published for resource {}: no stock left", event.resourceId()));
            }
        } catch (RuntimeException e) {
            log.error("Failed to process offer for resource {} after recommendation {} was {}",
                    event.resourceId(), event.recommendationId(), event.status(), e);
        }
    }

    private boolean priceWasChanged(RecommendationStatus status) {
        return status == RecommendationStatus.ACCEPTED || status == RecommendationStatus.ADJUSTED;
    }
}
