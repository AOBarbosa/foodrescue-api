package br.com.seudominio.foodrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.OfferService;
import br.com.seudominio.foodrescue.domain.dtos.CreateOfferRequest;
import br.com.seudominio.foodrescue.domain.dtos.OfferDTO;
import br.com.seudominio.foodrescue.domain.enums.OfferStatus;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.foodrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

class OfferControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);

    private OfferService offerService;
    private OfferController controller;
    private AuthenticatedPrincipal establishment;
    private AuthenticatedPrincipal consumer;

    @BeforeEach
    void setUp() {
        offerService = mock(OfferService.class);
        controller = new OfferController(offerService);
        establishment = new AuthenticatedPrincipal(1L, UserRole.ESTABLISHMENT);
        consumer = new AuthenticatedPrincipal(99L, UserRole.CONSUMER);
    }

    private OfferDTO offer(Long id, OfferStatus status) {
        return new OfferDTO(id, 10L, "Pao de queijo", new BigDecimal("10.00"), new BigDecimal("5.20"),
                20, NOW.withHour(22), status, false, NOW);
    }

    @Test
    void createUsesAuthenticatedEstablishment() {
        CreateOfferRequest request = new CreateOfferRequest(10L, null, null, null);
        OfferDTO published = offer(700L, OfferStatus.ACTIVE);
        when(offerService.createOffer(request, 1L)).thenReturn(published);

        ResponseEntity<ApiResponse<OfferDTO>> response = controller.create(request, establishment);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(published);
        assertThat(response.getBody().success()).isTrue();
        verify(offerService).createOffer(request, 1L);
    }

    @Test
    void listUsesAuthenticatedEstablishment() {
        List<OfferDTO> offers = List.of(offer(700L, OfferStatus.ACTIVE));
        when(offerService.findAllForEstablishment(1L)).thenReturn(offers);

        ResponseEntity<ApiResponse<List<OfferDTO>>> response = controller.list(establishment);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(offers);
        verify(offerService).findAllForEstablishment(1L);
    }

    @Test
    void availableDoesNotDependOnTheEstablishment() {
        List<OfferDTO> offers = List.of(offer(700L, OfferStatus.ACTIVE));
        when(offerService.findOrderable()).thenReturn(offers);

        ResponseEntity<ApiResponse<List<OfferDTO>>> response = controller.available();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(offers);
        verify(offerService).findOrderable();
    }

    @Test
    void findByIdUsesAuthenticatedEstablishment() {
        OfferDTO found = offer(700L, OfferStatus.ACTIVE);
        when(offerService.findByIdForEstablishment(700L, 1L)).thenReturn(found);

        ResponseEntity<ApiResponse<OfferDTO>> response = controller.findById(700L, establishment);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(found);
        verify(offerService).findByIdForEstablishment(700L, 1L);
    }

    @Test
    void closeUsesAuthenticatedEstablishment() {
        OfferDTO closed = offer(700L, OfferStatus.CLOSED);
        when(offerService.closeOffer(700L, 1L)).thenReturn(closed);

        ResponseEntity<ApiResponse<OfferDTO>> response = controller.close(700L, establishment);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status()).isEqualTo(OfferStatus.CLOSED);
        assertThat(response.getBody().message()).contains("closed");
        verify(offerService).closeOffer(700L, 1L);
    }

    @Test
    void consumerPrincipalIsNotUsedByTheAvailableEndpoint() {
        when(offerService.findOrderable()).thenReturn(List.of());

        assertThat(controller.available().getBody()).isNotNull();
        assertThat(consumer.role()).isEqualTo(UserRole.CONSUMER);
    }
}
