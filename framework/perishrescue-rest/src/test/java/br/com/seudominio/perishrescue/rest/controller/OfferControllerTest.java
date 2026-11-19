package br.com.seudominio.perishrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.services.OfferService;
import br.com.seudominio.perishrescue.domain.dtos.CreateOfferRequest;
import br.com.seudominio.perishrescue.domain.dtos.OfferDTO;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.perishrescue.rest.security.UserRole;

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
    private AuthenticatedPrincipal offerer;
    private AuthenticatedPrincipal beneficiary;

    @BeforeEach
    void setUp() {
        offerService = mock(OfferService.class);
        controller = new OfferController(offerService);
        offerer = new AuthenticatedPrincipal(1L, UserRole.OFFERER);
        beneficiary = new AuthenticatedPrincipal(99L, UserRole.BENEFICIARY);
    }

    private OfferDTO offer(Long id, OfferStatus status) {
        return new OfferDTO(id, 10L, "Pao de queijo", new BigDecimal("10.00"), new BigDecimal("5.20"),
                20, NOW.withHour(22), status, false, NOW);
    }

    @Test
    void createUsesAuthenticatedOfferer() {
        CreateOfferRequest request = new CreateOfferRequest(10L, null, null, null);
        OfferDTO published = offer(700L, OfferStatus.ACTIVE);
        when(offerService.createOffer(request, 1L)).thenReturn(published);

        ResponseEntity<ApiResponse<OfferDTO>> response = controller.create(request, offerer);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(published);
        assertThat(response.getBody().success()).isTrue();
        verify(offerService).createOffer(request, 1L);
    }

    @Test
    void listUsesAuthenticatedOfferer() {
        List<OfferDTO> offers = List.of(offer(700L, OfferStatus.ACTIVE));
        when(offerService.findAllForOfferer(1L)).thenReturn(offers);

        ResponseEntity<ApiResponse<List<OfferDTO>>> response = controller.list(offerer);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(offers);
        verify(offerService).findAllForOfferer(1L);
    }

    @Test
    void availableDoesNotDependOnTheOfferer() {
        List<OfferDTO> offers = List.of(offer(700L, OfferStatus.ACTIVE));
        when(offerService.findOrderable()).thenReturn(offers);

        ResponseEntity<ApiResponse<List<OfferDTO>>> response = controller.available();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(offers);
        verify(offerService).findOrderable();
    }

    @Test
    void findByIdUsesAuthenticatedOfferer() {
        OfferDTO found = offer(700L, OfferStatus.ACTIVE);
        when(offerService.findByIdForOfferer(700L, 1L)).thenReturn(found);

        ResponseEntity<ApiResponse<OfferDTO>> response = controller.findById(700L, offerer);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(found);
        verify(offerService).findByIdForOfferer(700L, 1L);
    }

    @Test
    void closeUsesAuthenticatedOfferer() {
        OfferDTO closed = offer(700L, OfferStatus.CLOSED);
        when(offerService.closeOffer(700L, 1L)).thenReturn(closed);

        ResponseEntity<ApiResponse<OfferDTO>> response = controller.close(700L, offerer);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().status()).isEqualTo(OfferStatus.CLOSED);
        assertThat(response.getBody().message()).contains("closed");
        verify(offerService).closeOffer(700L, 1L);
    }

    @Test
    void beneficiaryPrincipalIsNotUsedByTheAvailableEndpoint() {
        when(offerService.findOrderable()).thenReturn(List.of());

        assertThat(controller.available().getBody()).isNotNull();
        assertThat(beneficiary.role()).isEqualTo(UserRole.BENEFICIARY);
    }

    @Test
    void markAsPriorityUsesAuthenticatedOfferer() {
        OfferDTO prioritized = new OfferDTO(700L, 10L, "Pao de queijo", new BigDecimal("10.00"),
                new BigDecimal("5.20"), 20, NOW.withHour(22), OfferStatus.ACTIVE, true, NOW);
        when(offerService.markAsPriority(700L, 1L)).thenReturn(prioritized);

        ResponseEntity<ApiResponse<OfferDTO>> response = controller.markAsPriority(700L, offerer);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data().priority()).isTrue();
        assertThat(response.getBody().message()).contains("marked as priority");
        verify(offerService).markAsPriority(700L, 1L);
    }
}