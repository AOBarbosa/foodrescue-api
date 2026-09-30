package br.com.seudominio.foodrescue.business.aggregator;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.domain.dtos.PeriodDTO;
import br.com.seudominio.foodrescue.domain.dtos.WasteAndSavingsIndicatorsDTO;
import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

class IndicatorsAggregatorTest {

    private IndicatorsAggregator aggregator;
    private PeriodDTO period;

    @BeforeEach
    void setUp() {
        aggregator = new IndicatorsAggregator();
        period = new PeriodDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    }

    private Product createProduct(Long id, BigDecimal originalPrice) {
        Product product = new Product();
        product.setId(id);
        product.setName("Produto " + id);
        product.setOriginalPrice(Money.of(originalPrice));
        product.setCurrentPrice(Money.of(originalPrice));
        return product;
    }

    private Sale createSale(Product product, int quantity, BigDecimal unitPrice) {
        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setQuantity(quantity);
        sale.setUnitPrice(Money.of(unitPrice));
        return sale;
    }

    private AiRecommendation createRecommendation(Long id, RecommendationStatus status) {
        AiRecommendation recommendation = new AiRecommendation();
        recommendation.setId(id);
        recommendation.setType(RecommendationType.DISCOUNT);
        recommendation.setStatus(status);
        return recommendation;
    }

    @Test
    void aggregateWithNoDataReturnsZeroedIndicatorsWithoutException() {
        WasteAndSavingsIndicatorsDTO dto = aggregator.aggregate(period, Collections.emptyList(), Collections.emptyList());

        assertThat(dto).isNotNull();
        assertThat(dto.period()).isEqualTo(period);
        assertThat(dto.wasteAvoidedUnits()).isZero();
        assertThat(dto.recoveredRevenue()).isEqualByComparingTo("0.00");
        assertThat(dto.acceptedRecommendations()).isZero();
        assertThat(dto.refusedRecommendations()).isZero();
        assertThat(dto.adjustedRecommendations()).isZero();
        assertThat(dto.comparisonPeriod()).isNull();
    }

    @Test
    void aggregateWithNullListsReturnsZeroedIndicators() {
        WasteAndSavingsIndicatorsDTO dto = aggregator.aggregate(period, null, null);

        assertThat(dto).isNotNull();
        assertThat(dto.wasteAvoidedUnits()).isZero();
        assertThat(dto.recoveredRevenue()).isEqualByComparingTo("0.00");
        assertThat(dto.acceptedRecommendations()).isZero();
        assertThat(dto.refusedRecommendations()).isZero();
        assertThat(dto.adjustedRecommendations()).isZero();
    }

    @Test
    void aggregateSalesOnlyIncludesDiscountedSalesInWasteAvoidedAndRevenue() {
        Product p1 = createProduct(1L, new BigDecimal("10.00"));
        Product p2 = createProduct(2L, new BigDecimal("20.00"));
        Product p3 = createProduct(3L, new BigDecimal("15.00"));

        Sale saleDiscounted1 = createSale(p1, 5, new BigDecimal("7.00")); // 5 * 7 = 35.00
        Sale saleDiscounted2 = createSale(p2, 2, new BigDecimal("10.00")); // 2 * 10 = 20.00
        Sale saleFullPrice = createSale(p3, 10, new BigDecimal("15.00")); // Não é desconto

        List<Sale> sales = List.of(saleDiscounted1, saleDiscounted2, saleFullPrice);

        WasteAndSavingsIndicatorsDTO dto = aggregator.aggregate(period, sales, Collections.emptyList());

        assertThat(dto.wasteAvoidedUnits()).isEqualTo(7); // 5 + 2
        assertThat(dto.recoveredRevenue()).isEqualByComparingTo("55.00"); // 35.00 + 20.00
    }

    @Test
    void aggregateRecommendationsCountsAcceptedAdjustedAndRefusedCorrectly() {
        List<AiRecommendation> recommendations = List.of(
                createRecommendation(1L, RecommendationStatus.ACCEPTED),
                createRecommendation(2L, RecommendationStatus.ADJUSTED),
                createRecommendation(3L, RecommendationStatus.REFUSED),
                createRecommendation(4L, RecommendationStatus.PENDING),
                createRecommendation(5L, RecommendationStatus.EXPIRED)
        );

        WasteAndSavingsIndicatorsDTO dto = aggregator.aggregate(period, Collections.emptyList(), recommendations);

        assertThat(dto.acceptedRecommendations()).isEqualTo(2); // ACCEPTED + ADJUSTED
        assertThat(dto.adjustedRecommendations()).isEqualTo(1); // ADJUSTED
        assertThat(dto.refusedRecommendations()).isEqualTo(1); // REFUSED
    }

    @Test
    void aggregateWithComparisonPeriodPreservesNestedIndicators() {
        PeriodDTO prevPeriod = new PeriodDTO(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        WasteAndSavingsIndicatorsDTO comparisonDTO = WasteAndSavingsIndicatorsDTO.builder()
                .period(prevPeriod)
                .wasteAvoidedUnits(3)
                .recoveredRevenue(new BigDecimal("25.00"))
                .acceptedRecommendations(1)
                .refusedRecommendations(0)
                .adjustedRecommendations(0)
                .build();

        WasteAndSavingsIndicatorsDTO currentDTO =
                aggregator.aggregate(period, Collections.emptyList(), Collections.emptyList(), comparisonDTO);

        assertThat(currentDTO.comparisonPeriod()).isNotNull();
        assertThat(currentDTO.comparisonPeriod().period()).isEqualTo(prevPeriod);
        assertThat(currentDTO.comparisonPeriod().wasteAvoidedUnits()).isEqualTo(3);
        assertThat(currentDTO.comparisonPeriod().recoveredRevenue()).isEqualByComparingTo("25.00");
    }
}
