package br.com.seudominio.perishrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.services.DemandForecastService;
import br.com.seudominio.perishrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.perishrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class DemandForecastControllerTest {

    private DemandForecastService demandForecastService;
    private DemandForecastController controller;
    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        demandForecastService = mock(DemandForecastService.class);
        controller = new DemandForecastController(demandForecastService);
        principal = new AuthenticatedPrincipal(1L, UserRole.OFFERER);
    }

    @Test
    void predictUsesAuthenticatedOfferer() {
        DemandForecastResponse forecast = mock(DemandForecastResponse.class);
        when(demandForecastService.predictDemand(10L, 1L)).thenReturn(forecast);

        ResponseEntity<ApiResponse<DemandForecastResponse>> response = controller.predict(10L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(forecast);
        assertThat(response.getBody().success()).isTrue();
        verify(demandForecastService).predictDemand(10L, 1L);
    }

    @Test
    void findLatestUsesAuthenticatedOfferer() {
        DemandForecastResponse forecast = mock(DemandForecastResponse.class);
        when(demandForecastService.findLatest(10L, 1L)).thenReturn(forecast);

        ResponseEntity<ApiResponse<DemandForecastResponse>> response = controller.findLatest(10L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(forecast);
        verify(demandForecastService).findLatest(10L, 1L);
    }
}
