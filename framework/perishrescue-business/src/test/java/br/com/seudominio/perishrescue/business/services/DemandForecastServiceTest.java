package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.forecast.ForecastEligibilityChecker;
import br.com.seudominio.perishrescue.business.forecast.WeekdayHourlyAverageForecastStrategy;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.perishrescue.domain.entities.DemandForecast;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.entities.Sale;
import br.com.seudominio.perishrescue.domain.enums.ForecastConfidence;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.DemandForecastMapper;
import br.com.seudominio.perishrescue.persistence.repositories.DemandForecastRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ResourceRepository;
import br.com.seudominio.perishrescue.persistence.repositories.SaleRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

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
    private ResourceRepository resourceRepository;
    private SaleRepository saleRepository;
    private DemandForecastService service;
    private PerishableResource resource;

    @BeforeEach
    void setUp() {
        forecastRepository = mock(DemandForecastRepository.class);
        resourceRepository = mock(ResourceRepository.class);
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
                resourceRepository, saleRepository, new WeekdayHourlyAverageForecastStrategy(),
                new ForecastEligibilityChecker(5), timeProvider, "22:00", 4);

        Offerer offerer = new Offerer();
        offerer.setId(1L);
        resource = new TestResource();
        resource.setId(10L);
        resource.setOfferer(offerer);
        resource.setStockQuantity(20);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
    }

    private List<Sale> sameWeekdaySales(int weeks) {
        List<Sale> history = new ArrayList<>();
        for (int week = weeks; week >= 1; week--) {
            history.add(Sale.builder()
                    .resource(resource)
                    .quantity(4)
                    .unitPrice(Money.of(5.0))
                    .soldAt(NOW.minusWeeks(week).withHour(17))
                    .build());
            history.add(Sale.builder()
                    .resource(resource)
                    .quantity(2)
                    .unitPrice(Money.of(5.0))
                    .soldAt(NOW.minusWeeks(week).withHour(19))
                    .build());
        }
        return history;
    }

    @Test
    void predictDemandRecordsForecastAssociatedWithResourceAndCalculationMoment() {
        when(saleRepository.findByResourceAndPeriod(10L, START_OF_TODAY.minusWeeks(4), START_OF_TODAY))
                .thenReturn(sameWeekdaySales(4));

        DemandForecastResponse response = service.predictDemand(10L, 1L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.resourceId()).isEqualTo(10L);
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
        when(saleRepository.findByResourceAndPeriod(10L, START_OF_TODAY.minusWeeks(4), START_OF_TODAY))
                .thenReturn(sameWeekdaySales(2));

        assertThatThrownBy(() -> service.predictDemand(10L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("4 sales found")
                .hasMessageContaining("at least 5 required");
        verify(forecastRepository, never()).save(any(DemandForecast.class));
    }

    @Test
    void predictDemandRejectsResourceFromAnotherOfferer() {
        assertThatThrownBy(() -> service.predictDemand(10L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
        verify(forecastRepository, never()).save(any(DemandForecast.class));
    }

    @Test
    void predictDemandRejectsUnknownResource() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.predictDemand(99L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findLatestReturnsMostRecentForecast() {
        DemandForecast latest = DemandForecast.builder()
                .id(7L)
                .resource(resource)
                .predictedQuantity(3)
                .stockQuantity(20)
                .confidence(ForecastConfidence.MEDIUM)
                .sampleSize(4)
                .calculatedAt(NOW)
                .forecastUntil(NOW.withHour(22))
                .build();
        when(forecastRepository.findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.of(latest));

        DemandForecastResponse response = service.findLatest(10L, 1L);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.predictedQuantity()).isEqualTo(3);
        assertThat(response.confidence()).isEqualTo(ForecastConfidence.MEDIUM);
    }

    @Test
    void findLatestFailsWhenResourceWasNeverForecast() {
        when(forecastRepository.findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findLatest(10L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
