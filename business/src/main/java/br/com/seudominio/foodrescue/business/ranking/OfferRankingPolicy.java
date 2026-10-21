package br.com.seudominio.foodrescue.business.ranking;

import br.com.seudominio.foodrescue.domain.entities.Offer;

import java.util.Comparator;

/**
 * Policy for ordering offers in consumer listings (UC11).
 *
 * <p>Priority/featured offers come first, followed by standard offers.
 * Within each priority group, offers closer to expiration come first.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public class OfferRankingPolicy implements Comparator<Offer> {

    private static final Comparator<Offer> COMPARATOR = Comparator
            .comparing(Offer::isPriority, Comparator.reverseOrder())
            .thenComparing(Offer::getExpiresAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Offer::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    @Override
    public int compare(Offer o1, Offer o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return 1;
        }
        if (o2 == null) {
            return -1;
        }
        return COMPARATOR.compare(o1, o2);
    }
}
