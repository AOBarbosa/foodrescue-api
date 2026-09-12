package br.com.seudominio.foodrescue.domain.converter;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

/**
 * JPA converter between {@link Percentage} and the {@code numeric} column it's persisted as.
 * Keeps {@link Percentage} itself free of persistence concerns.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
@Converter
public class PercentageConverter implements AttributeConverter<Percentage, BigDecimal> {

    /**
     * Converts a {@link Percentage} attribute to its database column representation.
     *
     * @param attribute the attribute value, may be {@code null}
     * @return the column value, or {@code null} if {@code attribute} is {@code null}
     */
    @Override
    public BigDecimal convertToDatabaseColumn(Percentage attribute) {
        return attribute == null ? null : attribute.value();
    }

    /**
     * Converts a database column value back to a {@link Percentage} attribute.
     *
     * @param dbData the column value, may be {@code null}
     * @return the attribute value, or {@code null} if {@code dbData} is {@code null}
     */
    @Override
    public Percentage convertToEntityAttribute(BigDecimal dbData) {
        return dbData == null ? null : Percentage.of(dbData);
    }
}
