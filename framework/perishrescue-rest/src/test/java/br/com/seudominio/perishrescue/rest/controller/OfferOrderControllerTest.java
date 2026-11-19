package br.com.seudominio.perishrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.services.OfferOrderService;
import br.com.seudominio.perishrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.perishrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.perishrescue.domain.enums.OfferOrderStatus;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.perishrescue.rest.security.UserRole;

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
    private AuthenticatedPrincipal beneficiary;

    @BeforeEach
    void setUp() {
        offerOrderService = mock(OfferOrderService.class);
        controller = new OfferOrderController(offerOrderService);
        beneficiary = new AuthenticatedPrincipal(50L, UserRole.BENEFICIARY);
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
    void reserveUsesAuthenticatedBeneficiary() {
        ReserveOfferRequest request = new ReserveOfferRequest(700L, 2);
        OfferOrderDTO reserved = orderDto(900L);
        when(offerOrderService.reserve(request, 50L)).thenReturn(reserved);

        ResponseEntity<ApiResponse<OfferOrderDTO>> response = controller.reserve(request, beneficiary);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(reserved);
        assertThat(response.getBody().success()).isTrue();
        verify(offerOrderService).reserve(request, 50L);
    }

    @Test
    void listUsesAuthenticatedBeneficiary() {
        List<OfferOrderDTO> orders = List.of(orderDto(900L));
        when(offerOrderService.findAllForBeneficiary(50L)).thenReturn(orders);

        ResponseEntity<ApiResponse<List<OfferOrderDTO>>> response = controller.list(beneficiary);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(orders);
        verify(offerOrderService).findAllForBeneficiary(50L);
    }

    @Test
    void findByIdUsesAuthenticatedBeneficiary() {
        OfferOrderDTO found = orderDto(900L);
        when(offerOrderService.findByIdForBeneficiary(900L, 50L)).thenReturn(found);

        ResponseEntity<ApiResponse<OfferOrderDTO>> response = controller.findById(900L, beneficiary);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(found);
        verify(offerOrderService).findByIdForBeneficiary(900L, 50L);
    }
}
