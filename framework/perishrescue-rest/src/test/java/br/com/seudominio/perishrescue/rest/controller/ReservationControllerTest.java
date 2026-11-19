package br.com.seudominio.perishrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.services.ReservationService;
import br.com.seudominio.perishrescue.domain.dtos.ReservationDTO;
import br.com.seudominio.perishrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.perishrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

class ReservationControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);

    private ReservationService reservationService;
    private ReservationController controller;
    private AuthenticatedPrincipal beneficiary;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        controller = new ReservationController(reservationService);
        beneficiary = new AuthenticatedPrincipal(50L, UserRole.BENEFICIARY);
    }

    private ReservationDTO orderDto(Long id) {
        return new ReservationDTO(
                id,
                700L,
                10L,
                "Pao de queijo",
                50L,
                2,
                new BigDecimal("5.20"),
                new BigDecimal("10.40"),
                ReservationStatus.RESERVED,
                "ABC123",
                NOW.plusHours(2),
                null,
                NOW);
    }

    @Test
    void reserveUsesAuthenticatedBeneficiary() {
        ReserveOfferRequest request = new ReserveOfferRequest(700L, 2);
        ReservationDTO reserved = orderDto(900L);
        when(reservationService.reserve(request, 50L)).thenReturn(reserved);

        ResponseEntity<ApiResponse<ReservationDTO>> response = controller.reserve(request, beneficiary);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(reserved);
        assertThat(response.getBody().success()).isTrue();
        verify(reservationService).reserve(request, 50L);
    }

    @Test
    void listUsesAuthenticatedBeneficiary() {
        List<ReservationDTO> orders = List.of(orderDto(900L));
        when(reservationService.findAllForBeneficiary(50L)).thenReturn(orders);

        ResponseEntity<ApiResponse<List<ReservationDTO>>> response = controller.list(beneficiary);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(orders);
        verify(reservationService).findAllForBeneficiary(50L);
    }

    @Test
    void findByIdUsesAuthenticatedBeneficiary() {
        ReservationDTO found = orderDto(900L);
        when(reservationService.findByIdForBeneficiary(900L, 50L)).thenReturn(found);

        ResponseEntity<ApiResponse<ReservationDTO>> response = controller.findById(900L, beneficiary);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(found);
        verify(reservationService).findByIdForBeneficiary(900L, 50L);
    }
}
