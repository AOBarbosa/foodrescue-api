package br.com.seudominio.foodrescue.pricing;

import br.com.seudominio.foodrescue.domain.FoodItem;
import br.com.seudominio.perishrescue.business.pricing.PricingContext;
import br.com.seudominio.perishrescue.domain.entities.Offerer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class FoodItemPricingPolicyTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    private final FoodItemPricingPolicy policy = new FoodItemPricingPolicy();

    private FoodItem itemClosingAt20(boolean surpriseBag) {
        Offerer offerer = Offerer.builder().businessHours(LocalTime.of(7, 0), LocalTime.of(20, 0)).build();
        FoodItem item = FoodItem.builder().offerer(offerer).build();
        item.setSurpriseBag(surpriseBag);
        return item;
    }

    private java.math.BigDecimal discountAt(FoodItem item, int hour, int minute) {
        return policy.discountFor(new PricingContext(item, null, TODAY.atTime(hour, minute))).value();
    }

    @Test
    void regularItemsGetARegressiveDiscountByTheHoursUntilClosing() {
        FoodItem item = itemClosingAt20(false);

        assertThat(discountAt(item, 12, 0)).isEqualByComparingTo("0");
        assertThat(discountAt(item, 16, 0)).isEqualByComparingTo("30");   // 4 hours before closing
        assertThat(discountAt(item, 18, 59)).isEqualByComparingTo("30");
        assertThat(discountAt(item, 19, 0)).isEqualByComparingTo("70");   // 1 hour before closing
    }

    @Test
    void surpriseBagsAlwaysGetAFixed75PercentOff() {
        FoodItem bag = itemClosingAt20(true);

        assertThat(discountAt(bag, 8, 0)).isEqualByComparingTo("75");
        assertThat(discountAt(bag, 19, 30)).isEqualByComparingTo("75");
    }
}
