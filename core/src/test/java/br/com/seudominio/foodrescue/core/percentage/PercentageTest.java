package br.com.seudominio.foodrescue.core.percentage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PercentageTest {

    @Test
    void rejectsValueBelowZero() {
        assertThatThrownBy(() -> Percentage.of(-0.01))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsValueAboveHundred() {
        assertThatThrownBy(() -> Percentage.of(100.01))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsBoundaryValues() {
        assertThat(Percentage.of(0.0).value()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(Percentage.of(100.0).value()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void asFractionConvertsToDecimalRatio() {
        assertThat(Percentage.of(25.0).asFraction()).isEqualByComparingTo(BigDecimal.valueOf(0.25));
    }
}
