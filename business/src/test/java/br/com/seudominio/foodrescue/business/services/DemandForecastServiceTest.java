package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.forecast.ForecastEligibilityChecker;
import br.com.seudominio.foodrescue.business.forecast.WeekdayHourlyAverageForecastStrategy;
import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.foodrescue.domain.entities.DemandForecast;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.DemandForecastMapper;
import br.com.seudominio.foodrescue.persistence.repositories.DemandForecastRepository;
import br.com.seudominio.foodrescue.persistence.repositories.ProductRepository;
import br.com.seudominio.foodrescue.persistence.repositories.SaleRepository;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class DemandForecastServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);
    private static final LocalDateTime START_OF_TODAY = LocalDateTime.of(2026, 9, 14, 0, 0);

    private DemandForecastRepository forecastRepository;
    private ProductRepository productRepository;
    private SaleRepository saleRepository;
    private DemandForecastService service;
    private Product product;

    @BeforeEach
    void setUp() {
        forecastRepository = mock(DemandForecastRepository.class);
        productRepository = mock(ProductRepository.class);
        saleRepository = mock(SaleRepository.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);
        when(forecastRepository.save(any(DemandForecast.class))).thenAnswer(invocation -> {
            DemandForecast forecast = invocation.getArgument(0);
            forecast.setId(100L);
            return forecast;
        });

        service = new DemandForecastService(
                forecastRepository, new DemandForecastMapper(), mock(Validator.class), mock(MessageUtils.class),
                productRepository, saleRepository, new WeekdayHourlyAverageForecastStrategy(),
                new ForecastEligibilityChecker(5), timeProvider, "22:00", 4);

        Establishment establishment = new Establishment();
        establishment.setId(1L);
        product = new Product();
        product.setId(10L);
        product.setEstablishment(establishment);
        product.setStockQuantity(20);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
    }

    private List<Sale> sameWeekdaySales(int weeks) {
        List<Sale> history = new ArrayList<>();
        for (int week = weeks; week >= 1; week--) {
            history.add(Sale.builder()
                    .product(product)
                    .quantity(4)
                    .unitPrice(Money.of(5.0))
                    .soldAt(NOW.minusWeeks(week).withHour(17))
                    .build());
            history.add(Sale.builder()
                    .product(product)
                    .quantity(2)
                    .unitPrice(Money.of(5.0))
                    .soldAt(NOW.minusWeeks(week).withHour(19))
                    .build());
        }
        return history;
    }

    @Test
    void predictDemandRecordsForecastAssociatedWithProductAndCalculationMoment() {
        when(saleRepository.findByProductAndPeriod(10L, START_OF_TODAY.minusWeeks(4), START_OF_TODAY))
                .thenReturn(sameWeekdaySales(4));

        DemandForecastResponse response = service.predictDemand(10L, 1L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.productId()).isEqualTo(10L);
        assertThat(response.predictedQuantity()).isEqualTo(6);
        assertThat(response.stockQuantity()).isEqualTo(20);
        assertThat(response.confidence()).isEqualTo(ForecastConfidence.HIGH);
        assertThat(response.sampleSize()).isEqualTo(8);
        assertThat(response.source()).isEqualTo(WeekdayHourlyAverageForecastStrategy.SOURCE);
        assertThat(response.calculatedAt()).isEqualTo(NOW);
        assertThat(response.forecastUntil()).isEqualTo(LocalDateTime.of(2026, 9, 14, 22, 0));
        verify(forecastRepository).save(any(DemandForecast.class));
    }

    @Test
    void predictDemandRejectsInsufficientSalesHistoryWithoutRecordingForecast() {
        when(saleRepository.findByProductAndPeriod(10L, START_OF_TODAY.minusWeeks(4), START_OF_TODAY))
                .thenReturn(sameWeekdaySales(2));

        assertThatThrownBy(() -> service.predictDemand(10L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("4 sales found")
                .hasMessageContaining("at least 5 required");
        verify(forecastRepository, never()).save(any(DemandForecast.class));
    }

    @Test
    void predictDemandRejectsProductFromAnotherEstablishment() {
        assertThatThrownBy(() -> service.predictDemand(10L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(forecastRepository, never()).save(any(DemandForecast.class));
    }

    @Test
    void predictDemandRejectsUnknownProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.predictDemand(99L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findLatestReturnsMostRecentForecast() {
        DemandForecast latest = DemandForecast.builder()
                .id(7L)
                .product(product)
                .predictedQuantity(3)
                .stockQuantity(20)
                .confidence(ForecastConfidence.MEDIUM)
                .sampleSize(4)
                .calculatedAt(NOW)
                .forecastUntil(NOW.withHour(22))
                .build();
        when(forecastRepository.findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(latest));

        DemandForecastResponse response = service.findLatest(10L, 1L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.predictedQuantity()).isEqualTo(3);
        assertThat(response.confidence()).isEqualTo(ForecastConfidence.MEDIUM);
    }

    @Test
    void findLatestFailsWhenProductWasNeverForecast() {
        when(forecastRepository.findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findLatest(10L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
