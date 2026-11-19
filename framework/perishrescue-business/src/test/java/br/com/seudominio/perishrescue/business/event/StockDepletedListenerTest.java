package br.com.seudominio.perishrescue.business.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.services.OfferService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockDepletedListenerTest {

    private OfferService offerService;
    private StockDepletedListener listener;

    @BeforeEach
    void setUp() {
        offerService = mock(OfferService.class);
        listener = new StockDepletedListener(offerService);
    }

    @Test
    void closesTheOpenOffersOfTheDepletedResource() {
        when(offerService.closeOffersForDepletedResource(10L)).thenReturn(1);

        listener.onResourceStockDepleted(new ResourceStockDepletedEvent(10L, 1L));

        verify(offerService).closeOffersForDepletedResource(10L);
    }

    @Test
    void isSafeWhenTheResourceHadNoOpenOffer() {
        when(offerService.closeOffersForDepletedResource(10L)).thenReturn(0);

        assertThatCode(() -> listener.onResourceStockDepleted(new ResourceStockDepletedEvent(10L, 1L)))
                .doesNotThrowAnyException();
    }

    @Test
    void swallowsAFailureSoTheSaleIsNeverUndone() {
        when(offerService.closeOffersForDepletedResource(10L))
                .thenThrow(new IllegalStateException("database is down"));

        assertThatCode(() -> listener.onResourceStockDepleted(new ResourceStockDepletedEvent(10L, 1L)))
                .doesNotThrowAnyException();
    }
}
