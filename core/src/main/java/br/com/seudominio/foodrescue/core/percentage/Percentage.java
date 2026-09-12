package br.com.seudominio.foodrescue.core.percentage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object representing a percentage between 0 and 100 (inclusive).
 * Used to avoid duplicating discount validation across use cases.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public final class Percentage {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = BigDecimal.valueOf(100);
    private static final int SCALE = 2;

    private final BigDecimal value;

    private Percentage(BigDecimal value) {
        this.value = value;
    }

    /**
     * Creates a {@link Percentage} from a {@link BigDecimal} value.
     *
     * @param value the percentage value, between 0 and 100 (inclusive)
     * @return the created {@link Percentage}
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is outside the 0–100 range
     */
    public static Percentage of(BigDecimal value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.compareTo(MIN) < 0 || value.compareTo(MAX) > 0) {
            throw new IllegalArgumentException("percentage must be between 0 and 100, got: " + value);
        }
        return new Percentage(value.setScale(SCALE, RoundingMode.HALF_UP));
    }

    /**
     * Creates a {@link Percentage} from a {@code double} value.
     *
     * @param value the percentage value, between 0 and 100 (inclusive)
     * @return the created {@link Percentage}
     * @throws IllegalArgumentException if {@code value} is outside the 0–100 range
     */
    public static Percentage of(double value) {
        return of(BigDecimal.valueOf(value));
    }

    /**
     * Returns the raw percentage value (e.g. {@code 25} for 25%).
     *
     * @return the percentage value
     */
    public BigDecimal value() {
        return value;
    }

    /**
     * Returns the fractional representation of this percentage (e.g. 25% -&gt; 0.25),
     * useful for discount calculations.
     *
     * @return the fractional representation
     */
    public BigDecimal asFraction() {
        return value.divide(MAX, 4, RoundingMode.HALF_UP);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Percentage that)) {
            return false;
        }
        return value.compareTo(that.value) == 0;
    }

    @Override
    public int hashCode() {
        return value.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return value.toPlainString() + "%";
    }
}
