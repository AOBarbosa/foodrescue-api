package br.com.seudominio.foodrescue.core.money;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.seudominio.foodrescue.core.percentage.Percentage;
import java.math.BigDecimal;

import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-1)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void acceptsZero() {
        assertThat(Money.zero().isPositive()).isFalse();
    }

    @Test
    void isPositiveForAmountGreaterThanZero() {
        assertThat(Money.of(10.0).isPositive()).isTrue();
    }

    @Test
    void subtractRejectsNegativeResult() {
        Money five = Money.of(5.0);
        Money ten = Money.of(10.0);

        assertThatThrownBy(() -> five.subtract(ten))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void applyDiscountReducesAmountProportionally() {
        Money original = Money.of(100.0);

        Money discounted = original.applyDiscount(Percentage.of(25.0));

        assertThat(discounted).isEqualTo(Money.of(75.0));
    }

    @Test
    void equalsIgnoresScaleDifferences() {
        assertThat(Money.of(BigDecimal.valueOf(10))).isEqualTo(Money.of(BigDecimal.valueOf(10.00)));
    }
}
