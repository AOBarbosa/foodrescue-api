package br.com.seudominio.foodrescue.domain.enums;

import br.com.seudominio.foodrescue.domain.entities.DemandForecast;

/**
 * Confidence level of a {@link DemandForecast}, derived from how much
 * comparable sales history backed the calculation (UC05).
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public enum ForecastConfidence {

    LOW,
    MEDIUM,
    HIGH
}
