package br.com.seudominio.foodrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.SaleService;
import br.com.seudominio.foodrescue.domain.dtos.RegisterSaleDTO;
import br.com.seudominio.foodrescue.domain.dtos.SaleDTO;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.foodrescue.rest.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

class SaleControllerTest {

    private SaleService saleService;
    private SaleController saleController;
    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        saleService = mock(SaleService.class);
        saleController = new SaleController(saleService);
        principal = new AuthenticatedPrincipal(1L, UserRole.ESTABLISHMENT);
    }

    @Test
    void createRegistersSaleAndReturnsSuccessResponse() {
        RegisterSaleDTO dto = new RegisterSaleDTO(10L, 2, new BigDecimal("15.00"), null);
        SaleDTO created = new SaleDTO(100L, 10L, 2, new BigDecimal("15.00"), new BigDecimal("30.00"), LocalDateTime.now(), LocalDateTime.now());

        when(saleService.registerSale(dto, principal.id())).thenReturn(created);

        ResponseEntity<ApiResponse<SaleDTO>> response = saleController.create(dto, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("Sale registered successfully");
        assertThat(response.getBody().data()).isEqualTo(created);

        verify(saleService).registerSale(dto, principal.id());
    }

    @Test
    void findByIdReturnsSale() {
        SaleDTO sale = new SaleDTO(100L, 10L, 2, new BigDecimal("15.00"), new BigDecimal("30.00"), LocalDateTime.now(), LocalDateTime.now());
        when(saleService.findByIdForEstablishment(100L, principal.id())).thenReturn(sale);

        ResponseEntity<ApiResponse<SaleDTO>> response = saleController.findById(100L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("Sale retrieved successfully");
        assertThat(response.getBody().data()).isEqualTo(sale);

        verify(saleService).findByIdForEstablishment(100L, principal.id());
    }

    @Test
    void listReturnsSalesForProductAndPeriod() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 9, 26, 23, 59);
        SaleDTO sale = new SaleDTO(100L, 10L, 2, new BigDecimal("15.00"), new BigDecimal("30.00"), start.plusDays(1), LocalDateTime.now());

        when(saleService.findSalesByProductAndPeriod(10L, principal.id(), start, end)).thenReturn(List.of(sale));

        ResponseEntity<ApiResponse<List<SaleDTO>>> response = saleController.list(10L, start, end, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("Sales retrieved successfully");
        assertThat(response.getBody().data()).containsExactly(sale);

        verify(saleService).findSalesByProductAndPeriod(10L, principal.id(), start, end);
    }
}
