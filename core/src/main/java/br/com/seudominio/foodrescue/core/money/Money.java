package br.com.seudominio.foodrescue.core.money;

import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.core.validation.ValidationError;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * Value object representing a non-negative monetary amount.
 * Used to avoid duplicating price validation across use cases.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public final class Money {

    private static final int SCALE = 2;

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        this.amount = amount;
    }

    /**
     * Creates a {@link Money} from a {@link BigDecimal} amount.
     *
     * @param amount the amount, must not be negative
     * @return the created {@link Money}
     * @throws NullPointerException     if {@code amount} is {@code null}
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    public static Money of(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            ValidationError error = new ValidationError(
                    "amount must not be negative",
                    "amount",
                    amount,
                    "NEGATIVE_AMOUNT");
            throw new ValidationException("Invalid money amount", List.of(error));
        }
        return new Money(amount.setScale(SCALE, RoundingMode.HALF_UP));
    }

    /**
     * Creates a {@link Money} from a {@code double} amount.
     *
     * @param amount the amount, must not be negative
     * @return the created {@link Money}
     * @throws IllegalArgumentException if {@code amount} is negative
     */
    public static Money of(double amount) {
        return of(BigDecimal.valueOf(amount));
    }

    /**
     * Returns a zero-valued {@link Money} instance.
     *
     * @return a {@link Money} of zero
     */
    public static Money zero() {
        return new Money(BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP));
    }

    /**
     * Returns the underlying amount.
     *
     * @return the amount
     */
    public BigDecimal amount() {
        return amount;
    }

    /**
     * Checks whether this amount is strictly greater than zero.
     *
     * @return {@code true} if this amount is positive
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks whether this amount is strictly greater than another.
     *
     * @param other the amount to compare against
     * @return {@code true} if this amount is greater than {@code other}
     */
    public boolean isGreaterThan(Money other) {
        return amount.compareTo(other.amount) > 0;
    }

    /**
     * Checks whether this amount is less than or equal to another.
     *
     * @param other the amount to compare against
     * @return {@code true} if this amount is less than or equal to {@code other}
     */
    public boolean isLessThanOrEqualTo(Money other) {
        return amount.compareTo(other.amount) <= 0;
    }

    /**
     * Adds another amount to this one.
     *
     * @param other the amount to add
     * @return a new {@link Money} with the sum
     */
    public Money add(Money other) {
        return new Money(amount.add(other.amount));
    }

    /**
     * Subtracts another amount from this one.
     *
     * @param other the amount to subtract
     * @return a new {@link Money} with the difference
     * @throws IllegalArgumentException if the result would be negative
     */
    public Money subtract(Money other) {
        BigDecimal result = amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("subtraction result must not be negative");
        }
        return new Money(result);
    }

    /**
     * Applies a percentage discount, returning a new {@link Money} instance.
     *
     * @param percentage the discount to apply
     * @return a new {@link Money} with the discount applied
     * @throws NullPointerException if {@code percentage} is {@code null}
     */
    public Money applyDiscount(Percentage percentage) {
        Objects.requireNonNull(percentage, "percentage must not be null");
        BigDecimal factor = BigDecimal.ONE.subtract(percentage.asFraction());
        return Money.of(amount.multiply(factor));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money money)) {
            return false;
        }
        return amount.compareTo(money.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
