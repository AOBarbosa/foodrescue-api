package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.aggregator.IndicatorsAggregator;
import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.domain.dtos.PeriodDTO;
import br.com.seudominio.foodrescue.domain.dtos.WasteAndSavingsIndicatorsDTO;
import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.IndicatorPeriod;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.persistence.repositories.AiRecommendationRepository;
import br.com.seudominio.foodrescue.persistence.repositories.SaleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

class IndicatorsServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 12, 0, 0);
    private static final Long ESTABLISHMENT_ID = 1L;

    private SaleRepository saleRepository;
    private AiRecommendationRepository recommendationRepository;
    private IndicatorsAggregator aggregator;
    private TimeProvider timeProvider;
    private IndicatorsService service;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        recommendationRepository = mock(AiRecommendationRepository.class);
        aggregator = new IndicatorsAggregator();
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        service = new IndicatorsService(saleRepository, recommendationRepository, aggregator, timeProvider);
    }

    private Product createProduct(BigDecimal originalPrice) {
        Product product = new Product();
        product.setId(10L);
        product.setName("Pão");
        product.setOriginalPrice(Money.of(originalPrice));
        product.setCurrentPrice(Money.of(originalPrice));
        return product;
    }

    private Sale createSale(Product product, int quantity, BigDecimal unitPrice) {
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setQuantity(quantity);
        sale.setUnitPrice(Money.of(unitPrice));
        sale.setSoldAt(NOW);
        return sale;
    }

    @Test
    void getIndicatorsWithDefaultPeriodQueriesCurrentMonth() {
        when(saleRepository.findByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.findRespondedByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());

        WasteAndSavingsIndicatorsDTO dto = service.getIndicators(ESTABLISHMENT_ID, null, null, null, false);

        assertThat(dto).isNotNull();
        assertThat(dto.period().startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(dto.period().endDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(dto.wasteAvoidedUnits()).isZero();
        assertThat(dto.recoveredRevenue()).isEqualByComparingTo("0.00");

        ArgumentCaptor<LocalDateTime> startCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> endCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(saleRepository).findByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), startCaptor.capture(), endCaptor.capture());

        assertThat(startCaptor.getValue()).isEqualTo(LocalDate.of(2026, 9, 1).atStartOfDay());
        assertThat(endCaptor.getValue()).isEqualTo(LocalDate.of(2026, 9, 16).atStartOfDay());
    }

    @Test
    void getIndicatorsWithPeriodDayQueriesToday() {
        when(saleRepository.findByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.findRespondedByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());

        WasteAndSavingsIndicatorsDTO dto = service.getIndicators(ESTABLISHMENT_ID, IndicatorPeriod.DAY, null, null, false);

        assertThat(dto.period().startDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        assertThat(dto.period().endDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    void getIndicatorsWithPeriodWeekQueriesLastSevenDays() {
        when(saleRepository.findByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.findRespondedByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());

        WasteAndSavingsIndicatorsDTO dto = service.getIndicators(ESTABLISHMENT_ID, IndicatorPeriod.WEEK, null, null, false);

        assertThat(dto.period().startDate()).isEqualTo(LocalDate.of(2026, 9, 9));
        assertThat(dto.period().endDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    }

    @Test
    void getIndicatorsWithCustomDatesQueriesSpecifiedRange() {
        LocalDate start = LocalDate.of(2026, 9, 5);
        LocalDate end = LocalDate.of(2026, 9, 10);

        when(saleRepository.findByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());
        when(recommendationRepository.findRespondedByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());

        WasteAndSavingsIndicatorsDTO dto = service.getIndicators(ESTABLISHMENT_ID, null, start, end, false);

        assertThat(dto.period().startDate()).isEqualTo(start);
        assertThat(dto.period().endDate()).isEqualTo(end);
    }

    @Test
    void getIndicatorsWithStartDateAfterEndDateThrowsBusinessRuleViolationException() {
        LocalDate start = LocalDate.of(2026, 9, 20);
        LocalDate end = LocalDate.of(2026, 9, 10);

        assertThatThrownBy(() -> service.getIndicators(ESTABLISHMENT_ID, null, start, end, false))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("startDate must not be after endDate");
    }

    @Test
    void getIndicatorsWithCompareTrueQueriesPreviousPeriodAndAttachesComparison() {
        LocalDate start = LocalDate.of(2026, 9, 11);
        LocalDate end = LocalDate.of(2026, 9, 15); // 5 dias (11, 12, 13, 14, 15)

        Product product = createProduct(new BigDecimal("10.00"));
        Sale currentSale = createSale(product, 4, new BigDecimal("6.00")); // 4 * 6 = 24.00
        Sale prevSale = createSale(product, 2, new BigDecimal("5.00")); // 2 * 5 = 10.00

        when(saleRepository.findByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(List.of(currentSale))
                .thenReturn(List.of(prevSale));

        when(recommendationRepository.findRespondedByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(Collections.emptyList());

        WasteAndSavingsIndicatorsDTO dto = service.getIndicators(ESTABLISHMENT_ID, null, start, end, true);

        assertThat(dto).isNotNull();
        assertThat(dto.wasteAvoidedUnits()).isEqualTo(4);
        assertThat(dto.recoveredRevenue()).isEqualByComparingTo("24.00");

        assertThat(dto.comparisonPeriod()).isNotNull();
        assertThat(dto.comparisonPeriod().period().startDate()).isEqualTo(LocalDate.of(2026, 9, 6));
        assertThat(dto.comparisonPeriod().period().endDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(dto.comparisonPeriod().wasteAvoidedUnits()).isEqualTo(2);
        assertThat(dto.comparisonPeriod().recoveredRevenue()).isEqualByComparingTo("10.00");

        verify(saleRepository, times(2)).findByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any());
        verify(recommendationRepository, times(2))
                .findRespondedByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any());
    }

    @Test
    void getIndicatorsCalculatesSalesAndRecommendationsWithData() {
        Product product = createProduct(new BigDecimal("10.00"));
        Sale sale = createSale(product, 10, new BigDecimal("7.00"));

        AiRecommendation acceptedRec = new AiRecommendation();
        acceptedRec.setStatus(RecommendationStatus.ACCEPTED);

        AiRecommendation refusedRec = new AiRecommendation();
        refusedRec.setStatus(RecommendationStatus.REFUSED);

        when(saleRepository.findByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(List.of(sale));
        when(recommendationRepository.findRespondedByEstablishmentAndPeriod(eq(ESTABLISHMENT_ID), any(), any()))
                .thenReturn(List.of(acceptedRec, refusedRec));

        WasteAndSavingsIndicatorsDTO dto = service.getIndicators(ESTABLISHMENT_ID, IndicatorPeriod.DAY, null, null, false);

        assertThat(dto.wasteAvoidedUnits()).isEqualTo(10);
        assertThat(dto.recoveredRevenue()).isEqualByComparingTo("70.00");
        assertThat(dto.acceptedRecommendations()).isEqualTo(1);
        assertThat(dto.refusedRecommendations()).isEqualTo(1);
    }
}
