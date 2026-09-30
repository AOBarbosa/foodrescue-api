package br.com.seudominio.foodrescue.domain.dtos;

import java.time.LocalDate;

/**
 * Date range representing the period of the indicators (UC12).
 *
 * @param startDate start date of the period (inclusive)
 * @param endDate   end date of the period (inclusive)
 *
 * @author Clovis Luan
 * @since 1.0.0
 */
public record PeriodDTO(
        LocalDate startDate,
        LocalDate endDate) {
}
