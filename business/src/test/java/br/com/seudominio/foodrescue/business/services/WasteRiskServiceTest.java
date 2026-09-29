package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.risk.SurplusRatioRiskCalculator;
import br.com.seudominio.foodrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.foodrescue.domain.entities.DemandForecast;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.DemandForecastMapper;
import br.com.seudominio.foodrescue.persistence.repositories.DemandForecastRepository;
import br.com.seudominio.foodrescue.persistence.repositories.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class WasteRiskServiceTest {

    private static final LocalDateTime CALCULATED_AT = LocalDateTime.of(2026, 9, 14, 15, 0);

    private ProductRepository productRepository;
    private DemandForecastRepository forecastRepository;
    private WasteRiskService service;
    private Establishment establishment;
    private Product product;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        forecastRepository = mock(DemandForecastRepository.class);

        service = new WasteRiskService(productRepository, forecastRepository, new DemandForecastMapper(),
                new SurplusRatioRiskCalculator(), 70);

        establishment = new Establishment();
        establishment.setId(1L);
        product = product(10L, "Pão francês", 20);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    }

    private Product product(Long id, String name, int stockQuantity) {
        Product created = new Product();
        created.setId(id);
        created.setName(name);
        created.setEstablishment(establishment);
        created.setStockQuantity(stockQuantity);
        return created;
    }

    private DemandForecast forecast(Long id, Product forecastProduct, int predictedQuantity) {
        return DemandForecast.builder()
                .id(id)
                .product(forecastProduct)
                .predictedQuantity(predictedQuantity)
                .stockQuantity(forecastProduct.getStockQuantity())
                .confidence(ForecastConfidence.HIGH)
                .sampleSize(8)
                .source("weekday-hourly-average")
                .calculatedAt(CALCULATED_AT)
                .forecastUntil(CALCULATED_AT.withHour(22))
                .build();
    }

    @Test
    void assessFlagsProductWhoseRiskIsAboveTheThreshold() {
        when(forecastRepository.findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(forecast(100L, product, 4)));

        WasteRiskDTO risk = service.assess(10L, 1L);

        assertThat(risk.productId()).isEqualTo(10L);
        assertThat(risk.productName()).isEqualTo("Pão francês");
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
    void assessDoesNotFlagProductWhoseRiskOnlyReachesTheThreshold() {
        when(forecastRepository.findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(forecast(100L, product, 6)));

        WasteRiskDTO risk = service.assess(10L, 1L);

        assertThat(risk.riskPercentage()).isEqualByComparingTo("70.00");
        assertThat(risk.atRisk()).isFalse();
    }

    @Test
    void assessReturnsZeroRiskWhenStockIsCoveredByPredictedDemand() {
        when(forecastRepository.findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(forecast(100L, product, 20)));

        WasteRiskDTO risk = service.assess(10L, 1L);

        assertThat(risk.expectedSurplus()).isZero();
        assertThat(risk.riskPercentage()).isEqualByComparingTo("0.00");
        assertThat(risk.atRisk()).isFalse();
    }

    @Test
    void assessUsesCurrentStockSoASaleImmediatelyLowersTheRisk() {
        when(forecastRepository.findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(forecast(100L, product, 4)));

        product.setStockQuantity(5);
        WasteRiskDTO risk = service.assess(10L, 1L);

        assertThat(risk.stockQuantity()).isEqualTo(5);
        assertThat(risk.expectedSurplus()).isEqualTo(1);
        assertThat(risk.riskPercentage()).isEqualByComparingTo("20.00");
        assertThat(risk.atRisk()).isFalse();
    }

    @Test
    void assessRejectsProductFromAnotherEstablishment() {
        assertThatThrownBy(() -> service.assess(10L, 2L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assessRejectsUnknownProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assess(99L, 1L)).isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void assessFailsWhenProductWasNeverForecast() {
        when(forecastRepository.findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assess(10L, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("product 10");
    }

    @Test
    void findAllForEstablishmentKeepsOnlyFlaggedProductsRiskiestFirst() {
        Product safe = product(11L, "Leite", 10);
        Product risky = product(12L, "Bolo", 10);
        when(forecastRepository.findLatestByEstablishment(1L)).thenReturn(List.of(
                forecast(100L, product, 4), forecast(101L, safe, 9), forecast(102L, risky, 1)));

        List<WasteRiskDTO> risks = service.findAllForEstablishment(1L, true);

        assertThat(risks).extracting(WasteRiskDTO::productId).containsExactly(12L, 10L);
        assertThat(risks).extracting(WasteRiskDTO::riskPercentage)
                .usingComparatorForType(java.math.BigDecimal::compareTo, java.math.BigDecimal.class)
                .containsExactly(new java.math.BigDecimal("90.00"), new java.math.BigDecimal("80.00"));
    }

    @Test
    void findAllForEstablishmentCanReturnEveryForecastProduct() {
        Product safe = product(11L, "Leite", 10);
        when(forecastRepository.findLatestByEstablishment(1L)).thenReturn(List.of(
                forecast(100L, product, 4), forecast(101L, safe, 9)));

        List<WasteRiskDTO> risks = service.findAllForEstablishment(1L, false);

        assertThat(risks).extracting(WasteRiskDTO::productId).containsExactly(10L, 11L);
        assertThat(risks).extracting(WasteRiskDTO::atRisk).containsExactly(true, false);
    }
}
