package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.risk.SurplusRatioRiskCalculator;
import br.com.seudominio.perishrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.perishrescue.domain.entities.DemandForecast;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.ForecastConfidence;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.DemandForecastMapper;
import br.com.seudominio.perishrescue.persistence.repositories.DemandForecastRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ResourceRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class WasteRiskServiceTest {

    private static final LocalDateTime CALCULATED_AT = LocalDateTime.of(2026, 9, 14, 15, 0);

    private ResourceRepository resourceRepository;
    private DemandForecastRepository forecastRepository;
    private WasteRiskService service;
    private Offerer offerer;
    private PerishableResource resource;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        forecastRepository = mock(DemandForecastRepository.class);

        service = new WasteRiskService(resourceRepository, forecastRepository, new DemandForecastMapper(),
                new SurplusRatioRiskCalculator(), 70);

        offerer = new Offerer();
        offerer.setId(1L);
        resource = resource(10L, "Pão francês", 20);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
    }

    private PerishableResource resource(Long id, String name, int stockQuantity) {
        PerishableResource created = new TestResource();
        created.setId(id);
        created.setName(name);
        created.setOfferer(offerer);
        created.setStockQuantity(stockQuantity);
        return created;
    }

    private DemandForecast forecast(Long id, PerishableResource forecastResource, int predictedQuantity) {
        return DemandForecast.builder()
                .id(id)
                .resource(forecastResource)
                .predictedQuantity(predictedQuantity)
                .stockQuantity(forecastResource.getStockQuantity())
                .confidence(ForecastConfidence.HIGH)
                .sampleSize(8)
                .source("weekday-hourly-average")
                .calculatedAt(CALCULATED_AT)
                .forecastUntil(CALCULATED_AT.withHour(22))
                .build();
    }

    @Test
    void assessFlagsResourceWhoseRiskIsAboveTheThreshold() {
        when(forecastRepository.findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(forecast(100L, resource, 4)));

        WasteRiskDTO risk = service.assess(10L, 1L);

        assertThat(risk.resourceId()).isEqualTo(10L);
        assertThat(risk.resourceName()).isEqualTo("Pão francês");
        assertThat(risk.stockQuantity()).isEqualTo(20);
        assertThat(risk.predictedQuantity()).isEqualTo(4);
        assertThat(risk.expectedSurplus()).isEqualTo(16);
        assertThat(risk.riskPercentage()).isEqualByComparingTo("80.00");
        assertThat(risk.riskThreshold()).isEqualByComparingTo("70.00");
        assertThat(risk.atRisk()).isTrue();
        assertThat(risk.forecastId()).isEqualTo(100L);
        assertThat(risk.forecastCalculatedAt()).isEqualTo(CALCULATED_AT);
    }

    @Test
    void assessDoesNotFlagResourceWhoseRiskOnlyReachesTheThreshold() {
        when(forecastRepository.findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(forecast(100L, resource, 6)));

        WasteRiskDTO risk = service.assess(10L, 1L);

        assertThat(risk.riskPercentage()).isEqualByComparingTo("70.00");
        assertThat(risk.atRisk()).isFalse();
    }

    @Test
    void assessReturnsZeroRiskWhenStockIsCoveredByPredictedDemand() {
        when(forecastRepository.findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(forecast(100L, resource, 20)));

        WasteRiskDTO risk = service.assess(10L, 1L);

        assertThat(risk.expectedSurplus()).isZero();
        assertThat(risk.riskPercentage()).isEqualByComparingTo("0.00");
        assertThat(risk.atRisk()).isFalse();
    }

    @Test
    void assessUsesCurrentStockSoASaleImmediatelyLowersTheRisk() {
        when(forecastRepository.findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(forecast(100L, resource, 4)));

        resource.setStockQuantity(5);
        WasteRiskDTO risk = service.assess(10L, 1L);

        assertThat(risk.stockQuantity()).isEqualTo(5);
        assertThat(risk.expectedSurplus()).isEqualTo(1);
        assertThat(risk.riskPercentage()).isEqualByComparingTo("20.00");
        assertThat(risk.atRisk()).isFalse();
    }

    @Test
    void assessRejectsResourceFromAnotherOfferer() {
        assertThatThrownBy(() -> service.assess(10L, 2L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assessRejectsUnknownResource() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assess(99L, 1L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assessFailsWhenResourceWasNeverForecast() {
        when(forecastRepository.findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assess(10L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("resource 10");
    }

    @Test
    void findAllForOffererKeepsOnlyFlaggedResourcesRiskiestFirst() {
        PerishableResource safe = resource(11L, "Leite", 10);
        PerishableResource risky = resource(12L, "Bolo", 10);
        when(forecastRepository.findLatestByOfferer(1L)).thenReturn(List.of(
                forecast(100L, resource, 4), forecast(101L, safe, 9), forecast(102L, risky, 1)));

        List<WasteRiskDTO> risks = service.findAllForOfferer(1L, true);

        assertThat(risks).extracting(WasteRiskDTO::resourceId).containsExactly(12L, 10L);
        assertThat(risks).extracting(WasteRiskDTO::riskPercentage)
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactly(new java.math.BigDecimal("90.00"), new java.math.BigDecimal("80.00"));
    }

    @Test
    void findAllForOffererCanReturnEveryForecastResource() {
        PerishableResource safe = resource(11L, "Leite", 10);
        when(forecastRepository.findLatestByOfferer(1L)).thenReturn(List.of(
                forecast(100L, resource, 4), forecast(101L, safe, 9)));

        List<WasteRiskDTO> risks = service.findAllForOfferer(1L, false);

        assertThat(risks).extracting(WasteRiskDTO::resourceId).containsExactly(10L, 11L);
        assertThat(risks).extracting(WasteRiskDTO::atRisk).containsExactly(true, false);
    }
}
