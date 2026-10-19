package br.com.seudominio.foodrescue.business.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.OfferService;

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
    void closesTheOpenOffersOfTheDepletedProduct() {
        when(offerService.closeOffersForDepletedProduct(10L)).thenReturn(1);

        listener.onProductStockDepleted(new ProductStockDepletedEvent(10L, 1L));

        verify(offerService).closeOffersForDepletedProduct(10L);
    }

    @Test
    void isSafeWhenTheProductHadNoOpenOffer() {
        when(offerService.closeOffersForDepletedProduct(10L)).thenReturn(0);

        assertThatCode(() -> listener.onProductStockDepleted(new ProductStockDepletedEvent(10L, 1L)))
                .doesNotThrowAnyException();
    }

    @Test
    void swallowsAFailureSoTheSaleIsNeverUndone() {
        when(offerService.closeOffersForDepletedProduct(10L))
                .thenThrow(new IllegalStateException("database is down"));

        assertThatCode(() -> listener.onProductStockDepleted(new ProductStockDepletedEvent(10L, 1L)))
                .doesNotThrowAnyException();
    }
}
