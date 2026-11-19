package br.com.seudominio.perishrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.services.IndicatorsService;
import br.com.seudominio.perishrescue.domain.dtos.PeriodDTO;
import br.com.seudominio.perishrescue.domain.dtos.WasteAndSavingsIndicatorsDTO;
import br.com.seudominio.perishrescue.domain.enums.IndicatorPeriod;
import br.com.seudominio.perishrescue.rest.dtos.ApiResponse;
import br.com.seudominio.perishrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.perishrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

class IndicatorsControllerTest {

    private IndicatorsService indicatorsService;
    private IndicatorsController controller;
    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        indicatorsService = mock(IndicatorsService.class);
        controller = new IndicatorsController(indicatorsService);
        principal = new AuthenticatedPrincipal(1L, UserRole.OFFERER);
    }

    private WasteAndSavingsIndicatorsDTO dummyDTO() {
        return WasteAndSavingsIndicatorsDTO.builder()
                .period(new PeriodDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 15)))
                .wasteAvoidedUnits(10)
                .recoveredRevenue(new BigDecimal("50.00"))
                .acceptedRecommendations(2)
                .refusedRecommendations(1)
                .adjustedRecommendations(0)
                .build();
    }

    @Test
    void getIndicatorsUsesAuthenticatedPrincipalAndForwardsParameters() {
        WasteAndSavingsIndicatorsDTO expected = dummyDTO();
        when(indicatorsService.getIndicators(1L, IndicatorPeriod.MONTH, null, null, false))
                .thenReturn(expected);

        ResponseEntity<ApiResponse<WasteAndSavingsIndicatorsDTO>> response =
                controller.getIndicators(IndicatorPeriod.MONTH, null, null, false, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(expected);
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).contains("Indicators retrieved successfully");

        verify(indicatorsService).getIndicators(1L, IndicatorPeriod.MONTH, null, null, false);
    }

    @Test
    void getIndicatorsWithCustomDatesAndCompareForwardsAllArguments() {
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 10);
        WasteAndSavingsIndicatorsDTO expected = dummyDTO();

        when(indicatorsService.getIndicators(1L, null, start, end, true))
                .thenReturn(expected);

        ResponseEntity<ApiResponse<WasteAndSavingsIndicatorsDTO>> response =
                controller.getIndicators(null, start, end, true, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(expected);

        verify(indicatorsService).getIndicators(1L, null, start, end, true);
    }
}
