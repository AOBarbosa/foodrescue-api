package br.com.seudominio.foodrescue.domain;

import br.com.seudominio.perishrescue.domain.entities.Offerer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class FoodItemTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 15, 0);

    private FoodItem itemOfOffererClosingAt(LocalTime closingTime) {
        Offerer offerer = Offerer.builder()
                .name("Padaria")
                .businessHours(LocalTime.of(7, 0), closingTime)
                .build();
        return FoodItem.builder().offerer(offerer).name("Pao").build();
    }

    @Test
    void criticalDeadlineIsTodaysClosingTimeWhenNoExpirationDateIsDeclared() {
        FoodItem item = itemOfOffererClosingAt(LocalTime.of(20, 0));

        assertThat(item.criticalDeadline(NOW)).isEqualTo(LocalDateTime.of(2026, 11, 18, 20, 0));
    }

    @Test
    void criticalDeadlineIsTodaysClosingTimeWhenTheItemExpiresLater() {
        FoodItem item = itemOfOffererClosingAt(LocalTime.of(20, 0));
        item.setExpirationDate(LocalDate.of(2026, 11, 20));

        assertThat(item.criticalDeadline(NOW)).isEqualTo(LocalDateTime.of(2026, 11, 18, 20, 0));
    }

    @Test
    void criticalDeadlineIsTheEndOfTheExpirationDateWhenItComesFirst() {
        FoodItem item = itemOfOffererClosingAt(LocalTime.of(20, 0));
        item.setExpirationDate(LocalDate.of(2026, 11, 17));

        assertThat(item.criticalDeadline(NOW)).isEqualTo(LocalDate.of(2026, 11, 17).atTime(LocalTime.MAX));
    }

    @Test
    void storesAllergensAsACopyAndTreatsNullAsNone() {
        FoodItem item = new FoodItem();
        EnumSet<Allergen> allergens = EnumSet.of(Allergen.GLUTEN);
        item.setAllergens(allergens);
        allergens.add(Allergen.NUTS);

        assertThat(item.getAllergens()).containsExactly(Allergen.GLUTEN);

        item.setAllergens(null);
        assertThat(item.getAllergens()).isEmpty();
    }

    @Test
    void theForecastOfAFoodItemRunsUntilItsOwnOffererClosesNotAFixedHour() {
        FoodItem item = itemOfOffererClosingAt(LocalTime.of(18, 0));
        var horizon = new br.com.seudominio.perishrescue.business.forecast.DeadlineForecastHorizon(
                java.time.temporal.ChronoUnit.HOURS);

        var window = horizon.windowFor(item, NOW);

        assertThat(window.until()).isEqualTo(LocalDateTime.of(2026, 11, 18, 18, 0));
        assertThat(window.remainingUnits()).isEqualTo(3);
    }
}
