package br.com.seudominio.foodrescue.domain.converter;

import br.com.seudominio.foodrescue.core.money.Money;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * JPA converter between {@link Money} and the {@code numeric} column it's persisted as.
 * Keeps {@link Money} itself free of persistence concerns.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
@Converter
public class MoneyConverter implements AttributeConverter<Money, BigDecimal> {

    /**
     * Converts a {@link Money} attribute to its database column representation.
     *
     * @param attribute the attribute value, may be {@code null}
     * @return the column value, or {@code null} if {@code attribute} is {@code null}
     */
    @Override
    public BigDecimal convertToDatabaseColumn(Money attribute) {
        return attribute == null ? null : attribute.amount();
    }

    /**
     * Converts a database column value back to a {@link Money} attribute.
     *
     * @param dbData the column value, may be {@code null}
     * @return the attribute value, or {@code null} if {@code dbData} is {@code null}
     */
    @Override
    public Money convertToEntityAttribute(BigDecimal dbData) {
        return dbData == null ? null : Money.of(dbData);
    }
}
