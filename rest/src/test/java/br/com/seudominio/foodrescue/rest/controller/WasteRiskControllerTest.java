package br.com.seudominio.foodrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.WasteRiskService;
import br.com.seudominio.foodrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.foodrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;

class WasteRiskControllerTest {

    private WasteRiskService wasteRiskService;
    private WasteRiskController controller;
    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        wasteRiskService = mock(WasteRiskService.class);
        controller = new WasteRiskController(wasteRiskService);
        principal = new AuthenticatedPrincipal(1L, UserRole.ESTABLISHMENT);
    }

    @Test
    void assessUsesAuthenticatedEstablishment() {
        WasteRiskDTO risk = mock(WasteRiskDTO.class);
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk);

        ResponseEntity<ApiResponse<WasteRiskDTO>> response = controller.assess(10L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(risk);
        assertThat(response.getBody().success()).isTrue();
        verify(wasteRiskService).assess(10L, 1L);
    }

    @Test
    void listForwardsTheAtRiskOnlyFilterOfTheAuthenticatedEstablishment() {
        List<WasteRiskDTO> risks = List.of(mock(WasteRiskDTO.class));
        when(wasteRiskService.findAllForEstablishment(1L, false)).thenReturn(risks);

        ResponseEntity<ApiResponse<List<WasteRiskDTO>>> response = controller.list(false, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(risks);
        verify(wasteRiskService).findAllForEstablishment(1L, false);
    }
}
