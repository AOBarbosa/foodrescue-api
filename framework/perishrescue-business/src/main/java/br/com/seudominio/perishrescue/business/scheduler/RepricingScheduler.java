package br.com.seudominio.perishrescue.business.scheduler;

import br.com.seudominio.perishrescue.business.services.OfferService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reapplies the pricing rules (hot spot 2) to every open offer, since the
 * discount grows as an item's deadline approaches. Runs every
 * {@code perishrescue.pricing.repricing-interval-ms} (default: five minutes).
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class RepricingScheduler {

    private static final Logger log = LoggerFactory.getLogger(RepricingScheduler.class);

    private final OfferService offerService;

    /**
     * Constructor.
     *
     * @param offerService the offer service
     */
    public RepricingScheduler(OfferService offerService) {
        this.offerService = offerService;
    }

    /**
     * Lowers the price of every open offer whose discount grew.
     */
    @Scheduled(fixedDelayString = "${perishrescue.pricing.repricing-interval-ms:300000}")
    public void reprice() {
        int repriced = offerService.repriceOpenOffers();
        if (repriced > 0) {
            log.info("Repricing: {} offer(s) got a lower price", repriced);
        }
    }
}
