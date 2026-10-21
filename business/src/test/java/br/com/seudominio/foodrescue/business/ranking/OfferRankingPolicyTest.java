package br.com.seudominio.foodrescue.business.ranking;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.domain.entities.Offer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

class OfferRankingPolicyTest {

    private final OfferRankingPolicy policy = new OfferRankingPolicy();
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);

    private Offer offer(Long id, boolean priority, LocalDateTime expiresAt) {
        Offer offer = Offer.builder()
                .id(id)
                .priority(priority)
                .expiresAt(expiresAt)
                .build();
        return offer;
    }

    @Test
    void ranksPriorityOffersBeforeRegularOffers() {
        Offer regular = offer(1L, false, NOW.plusHours(1));
        Offer priority = offer(2L, true, NOW.plusHours(5));

        List<Offer> offers = new ArrayList<>(List.of(regular, priority));
        offers.sort(policy);

        assertThat(offers).containsExactly(priority, regular);
    }

    @Test
    void ranksOffersCloserToExpirationFirstWithinSamePriority() {
        Offer urgentPriority = offer(1L, true, NOW.plusHours(1));
        Offer laterPriority = offer(2L, true, NOW.plusHours(3));
        Offer urgentRegular = offer(3L, false, NOW.plusHours(1));
        Offer laterRegular = offer(4L, false, NOW.plusHours(4));

        List<Offer> offers = new ArrayList<>(List.of(laterRegular, laterPriority, urgentRegular, urgentPriority));
        offers.sort(policy);

        assertThat(offers).containsExactly(urgentPriority, laterPriority, urgentRegular, laterRegular);
    }
}
