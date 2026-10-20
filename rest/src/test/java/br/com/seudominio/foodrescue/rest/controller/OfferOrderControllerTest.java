package br.com.seudominio.foodrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.OfferOrderService;
import br.com.seudominio.foodrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.foodrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.foodrescue.domain.enums.OfferOrderStatus;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.foodrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

class OfferOrderControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);

    private OfferOrderService offerOrderService;
    private OfferOrderController controller;
    private AuthenticatedPrincipal consumer;

    @BeforeEach
    void setUp() {
        offerOrderService = mock(OfferOrderService.class);
        controller = new OfferOrderController(offerOrderService);
        consumer = new AuthenticatedPrincipal(50L, UserRole.CONSUMER);
    }

    private OfferOrderDTO orderDto(Long id) {
        return new OfferOrderDTO(
                id,
                700L,
                10L,
                "Pao de queijo",
                50L,
                2,
                new BigDecimal("5.20"),
                new BigDecimal("10.40"),
                OfferOrderStatus.RESERVED,
                NOW);
    }

    @Test
    void reserveUsesAuthenticatedConsumer() {
        ReserveOfferRequest request = new ReserveOfferRequest(700L, 2);
        OfferOrderDTO reserved = orderDto(900L);
        when(offerOrderService.reserve(request, 50L)).thenReturn(reserved);

        ResponseEntity<ApiResponse<OfferOrderDTO>> response = controller.reserve(request, consumer);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(reserved);
        assertThat(response.getBody().success()).isTrue();
        verify(offerOrderService).reserve(request, 50L);
    }

    @Test
    void listUsesAuthenticatedConsumer() {
        List<OfferOrderDTO> orders = List.of(orderDto(900L));
        when(offerOrderService.findAllForConsumer(50L)).thenReturn(orders);

        ResponseEntity<ApiResponse<List<OfferOrderDTO>>> response = controller.list(consumer);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(orders);
        verify(offerOrderService).findAllForConsumer(50L);
    }

    @Test
    void findByIdUsesAuthenticatedConsumer() {
        OfferOrderDTO found = orderDto(900L);
        when(offerOrderService.findByIdForConsumer(900L, 50L)).thenReturn(found);

        ResponseEntity<ApiResponse<OfferOrderDTO>> response = controller.findById(900L, consumer);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(found);
        verify(offerOrderService).findByIdForConsumer(900L, 50L);
    }
}
