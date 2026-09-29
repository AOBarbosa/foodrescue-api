package br.com.seudominio.foodrescue.domain.mappers;

import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.foodrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.foodrescue.domain.entities.DemandForecast;
import br.com.seudominio.foodrescue.domain.entities.Product;
import org.springframework.stereotype.Component;

/**
 * Mapper between {@link DemandForecast} and {@link DemandForecastResponse},
 * plus the {@link WasteRiskDTO} view that combines a forecast with the
 * product's current stock (UC06).
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

    /**
     * Builds the waste risk view of a product (UC06) from the forecast it is
     * based on and the risk already calculated by the business layer.
     *
     * @param product         the assessed product, read for its name and current stock
     * @param forecast        the forecast the assessment is based on
     * @param expectedSurplus the units expected to be left over at closing time
     * @param riskPercentage  the calculated waste risk
     * @param riskThreshold   the percentage above which a product is flagged
     * @param atRisk          whether the risk is above the threshold
     * @return the corresponding DTO, or {@code null} if {@code product} or {@code forecast} is {@code null}
     */
    public WasteRiskDTO toRiskResponse(Product product, DemandForecast forecast, int expectedSurplus,
                                       Percentage riskPercentage, Percentage riskThreshold, boolean atRisk) {
        if (product == null || forecast == null) {
            return null;
        }
        return new WasteRiskDTO(
                product.getId(),
                product.getName(),
                product.getStockQuantity(),
                forecast.getPredictedQuantity(),
                expectedSurplus,
                riskPercentage == null ? null : riskPercentage.value(),
                riskThreshold == null ? null : riskThreshold.value(),
                atRisk,
                forecast.getId(),
                forecast.getCalculatedAt()
        );
    }
}
