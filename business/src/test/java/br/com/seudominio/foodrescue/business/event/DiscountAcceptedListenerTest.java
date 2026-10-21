package br.com.seudominio.foodrescue.business.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.OfferService;
import br.com.seudominio.foodrescue.domain.dtos.OfferDTO;
import br.com.seudominio.foodrescue.domain.enums.OfferStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

class DiscountAcceptedListenerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);

    private OfferService offerService;
    private DiscountAcceptedListener listener;

    @BeforeEach
    void setUp() {
        offerService = mock(OfferService.class);
        listener = new DiscountAcceptedListener(offerService);
    }

    private DiscountRecommendationRespondedEvent event(RecommendationStatus status) {
        return new DiscountRecommendationRespondedEvent(
                500L, 10L, 1L, status, new BigDecimal("48.00"), RecommendationType.DISCOUNT, NOW);
    }

    private DiscountRecommendationRespondedEvent surplusDestinationEvent(RecommendationStatus status) {
        return new DiscountRecommendationRespondedEvent(
                500L, 10L, 1L, status, null, RecommendationType.SURPLUS_DESTINATION, NOW);
    }

    private OfferDTO offer() {
        return new OfferDTO(700L, 10L, "Pao de queijo", new BigDecimal("10.00"), new BigDecimal("5.20"),
                20, NOW.withHour(22), OfferStatus.ACTIVE, false, NOW);
    }

    @Test
    void publishesAnOfferWhenTheDiscountIsAccepted() {
        when(offerService.createFromAcceptedDiscount(10L, 1L)).thenReturn(Optional.of(offer()));

        listener.onDiscountRecommendationResponded(event(RecommendationStatus.ACCEPTED));

        verify(offerService).createFromAcceptedDiscount(10L, 1L);
    }

    @Test
    void publishesAnOfferWhenTheDiscountIsAdjusted() {
        when(offerService.createFromAcceptedDiscount(10L, 1L)).thenReturn(Optional.of(offer()));

        listener.onDiscountRecommendationResponded(event(RecommendationStatus.ADJUSTED));

        verify(offerService).createFromAcceptedDiscount(10L, 1L);
    }

    @Test
    void publishesNothingWhenTheDiscountIsRefused() {
        listener.onDiscountRecommendationResponded(event(RecommendationStatus.REFUSED));

        verify(offerService, never()).createFromAcceptedDiscount(anyLong(), anyLong());
    }

    @Test
    void publishesNothingWhenTheRecommendationExpired() {
        listener.onDiscountRecommendationResponded(event(RecommendationStatus.EXPIRED));

        verify(offerService, never()).createFromAcceptedDiscount(anyLong(), anyLong());
    }

    @Test
    void toleratesAProductThatRanOutOfStockBeforeTheOfferCouldBePublished() {
        when(offerService.createFromAcceptedDiscount(10L, 1L)).thenReturn(Optional.empty());

        assertThatCode(() -> listener.onDiscountRecommendationResponded(event(RecommendationStatus.ACCEPTED)))
                .doesNotThrowAnyException();
    }

    @Test
    void swallowsAFailureSoTheAcceptedDiscountIsNeverUndone() {
        when(offerService.createFromAcceptedDiscount(10L, 1L))
                .thenThrow(new IllegalStateException("database is down"));

        assertThatCode(() -> listener.onDiscountRecommendationResponded(event(RecommendationStatus.ACCEPTED)))
                .doesNotThrowAnyException();
    }

    @Test
    void marksOrPublishesPriorityOfferWhenSurplusDestinationIsAccepted() {
        when(offerService.markOrPublishPriorityOfferForProduct(10L, 1L))
                .thenReturn(Optional.of(offer()));

        listener.onDiscountRecommendationResponded(surplusDestinationEvent(RecommendationStatus.ACCEPTED));

        verify(offerService).markOrPublishPriorityOfferForProduct(10L, 1L);
        verify(offerService, never()).createFromAcceptedDiscount(anyLong(), anyLong());
    }

    @Test
    void doesNothingWhenSurplusDestinationIsRefused() {
        listener.onDiscountRecommendationResponded(surplusDestinationEvent(RecommendationStatus.REFUSED));

        verify(offerService, never()).markOrPublishPriorityOfferForProduct(anyLong(), anyLong());
        verify(offerService, never()).createFromAcceptedDiscount(anyLong(), anyLong());
    }
}