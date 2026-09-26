package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.foodrescue.domain.entities.DemandForecast;
import org.springframework.stereotype.Component;

/**
 * Mapper between {@link DemandForecast} and {@link DemandForecastResponse}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class DemandForecastMapper implements DTOMapper<DemandForecast, DemandForecastResponse> {

    /**
     * Converts a {@link DemandForecastResponse} to a {@link DemandForecast}.
     * The product association is not resolved here.
     *
     * @param dto the DTO to convert
     * @return the corresponding entity, or {@code null} if {@code dto} is {@code null}
     */
    @Override
    public DemandForecast toEntity(DemandForecastResponse dto) {
        if (dto == null) {
            return null;
        }

        return DemandForecast.builder()
                .id(dto.id())
                .predictedQuantity(dto.predictedQuantity())
                .stockQuantity(dto.stockQuantity())
                .confidence(dto.confidence())
                .sampleSize(dto.sampleSize())
                .source(dto.source())
                .rationale(dto.rationale())
                .calculatedAt(dto.calculatedAt())
                .forecastUntil(dto.forecastUntil())
                .build();
    }

    /**
     * Converts a {@link DemandForecast} to a {@link DemandForecastResponse}.
     *
     * @param forecast the entity to convert
     * @return the corresponding DTO, or {@code null} if {@code forecast} is {@code null}
     */
    @Override
    public DemandForecastResponse toDto(DemandForecast forecast) {
        if (forecast == null) {
            return null;
        }
        return new DemandForecastResponse(
                forecast.getId(),
                forecast.getProduct().getId(),
                forecast.getPredictedQuantity(),
                forecast.getStockQuantity(),
                forecast.getConfidence(),
                forecast.getSampleSize(),
                forecast.getSource(),
                forecast.getRationale(),
                forecast.getCalculatedAt(),
                forecast.getForecastUntil()
        );
    }
}
