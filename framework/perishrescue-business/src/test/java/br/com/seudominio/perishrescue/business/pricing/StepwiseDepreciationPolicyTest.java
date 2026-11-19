package br.com.seudominio.perishrescue.business.pricing;

import br.com.seudominio.perishrescue.business.pricing.StepwiseDepreciationPolicy.TimeReference;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StepwiseDepreciationPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    /** TestResource's deadline is the end of its expiration date (here: 23:59:59.999 today). */
    private TestResource expiringToday() {
        TestResource resource = TestResource.builder().build();
        resource.setExpirationDate(TODAY);
        return resource;
    }

    private Percentage discountAt(PricingPolicy policy, TestResource resource, LocalDateTime now) {
        return policy.discountFor(new PricingContext(resource, null, now));
    }

    @Test
    void untilDeadlineGrowsTheDiscountAsTheDeadlineApproaches() {
        PricingPolicy foodLike = new StepwiseDepreciationPolicy(TimeReference.UNTIL_DEADLINE, ChronoUnit.HOURS,
                Map.of(4L, Percentage.of(30), 1L, Percentage.of(70)));
        TestResource resource = expiringToday();

        assertThat(discountAt(foodLike, resource, TODAY.atTime(12, 0)).value()).isZero();
        assertThat(discountAt(foodLike, resource, TODAY.atTime(20, 30)).value()).isEqualByComparingTo("30");
        assertThat(discountAt(foodLike, resource, TODAY.atTime(23, 30)).value()).isEqualByComparingTo("70");
    }

    @Test
    void sinceStartGrowsTheDiscountWithTheDaysElapsed() {
        PricingPolicy floraLike = new StepwiseDepreciationPolicy(TimeReference.SINCE_START, ChronoUnit.DAYS,
                Map.of(1L, Percentage.of(15), 3L, Percentage.of(50), 5L, Percentage.of(80)));
        TestResource resource = TestResource.builder().build();
        resource.setCreationDate(TODAY.atStartOfDay());

        assertThat(discountAt(floraLike, resource, TODAY.atTime(10, 0)).value()).isZero();
        assertThat(discountAt(floraLike, resource, TODAY.plusDays(1).atTime(10, 0)).value()).isEqualByComparingTo("15");
        assertThat(discountAt(floraLike, resource, TODAY.plusDays(4).atTime(10, 0)).value()).isEqualByComparingTo("50");
        assertThat(discountAt(floraLike, resource, TODAY.plusDays(6).atTime(10, 0)).value()).isEqualByComparingTo("80");
    }

    @Test
    void withoutStepsItGrantsNoDiscount() {
        PricingPolicy none = new StepwiseDepreciationPolicy(TimeReference.UNTIL_DEADLINE, ChronoUnit.HOURS, Map.of());

        assertThat(discountAt(none, expiringToday(), TODAY.atTime(23, 59)).value()).isZero();
    }

    @Test
    void parsesStepsWrittenAsThresholdPercentPairs() {
        assertThat(StepwiseDepreciationPolicy.parseSteps(" 4:30, 1:70 "))
                .containsEntry(4L, Percentage.of(30))
                .containsEntry(1L, Percentage.of(70));
        assertThat(StepwiseDepreciationPolicy.parseSteps("")).isEmpty();
        assertThatThrownBy(() -> StepwiseDepreciationPolicy.parseSteps("4-30"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
