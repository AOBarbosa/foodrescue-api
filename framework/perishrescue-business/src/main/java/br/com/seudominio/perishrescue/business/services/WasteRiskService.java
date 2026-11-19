package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.risk.RiskCalculator;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.perishrescue.domain.entities.DemandForecast;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.DemandForecastMapper;
import br.com.seudominio.perishrescue.persistence.repositories.DemandForecastRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ResourceRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that identifies resources at risk of being wasted (UC06).
 *
 * <p>It only combines an already existing demand forecast (produced by UC05)
 * with the resource's current stock — it never recalculates the forecast, nor
 * decides on a discount (that is UC07). The formula itself lives in a
 * {@link RiskCalculator}, and the flagging threshold is read from
 * {@code app.waste-risk.threshold}, so neither can change this class.</p>
 *
 * <p>The assessment is derived on read from the resource's <em>current</em>
 * stock, so every sale (UC04) or inventory update (UC03) is automatically
 * reflected the next time the risk is read — there is no stale value to
 * reprocess.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class WasteRiskService {

    private final ResourceRepository resourceRepository;
    private final DemandForecastRepository forecastRepository;
    private final DemandForecastMapper forecastMapper;
    private final RiskCalculator riskCalculator;
    private final Percentage threshold;

    /**
     * Constructor.
     *
     * @param resourceRepository  the resource repository
     * @param forecastRepository the demand forecast repository
     * @param forecastMapper     the demand forecast mapper
     * @param riskCalculator     the waste risk formula
     * @param threshold          the risk percentage above which a resource is flagged
     */
    public WasteRiskService(
            ResourceRepository resourceRepository,
            DemandForecastRepository forecastRepository,
            DemandForecastMapper forecastMapper,
            RiskCalculator riskCalculator,
            @Value("${app.waste-risk.threshold:70}") double threshold) {
        this.resourceRepository = resourceRepository;
        this.forecastRepository = forecastRepository;
        this.forecastMapper = forecastMapper;
        this.riskCalculator = riskCalculator;
        this.threshold = Percentage.of(threshold);
    }

    /**
     * Assesses the waste risk of a single resource from its latest forecast.
     *
     * @param resourceId       the resource id
     * @param offererId the authenticated offerer id
     * @return the resource's waste risk
     * @throws EntityNotFoundException if the resource does not exist, belongs to another
     *                                 offerer or was never forecast
     */
    public WasteRiskDTO assess(Long resourceId, Long offererId) {
        PerishableResource resource = findResourceOwnedBy(resourceId, offererId);
        DemandForecast forecast = forecastRepository
                .findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(resourceId)
                .orElseThrow(() -> new EntityNotFoundException(DemandForecast.class, "resource " + resourceId));

        return toRisk(resource, forecast);
    }

    /**
     * Lists the waste risk of every forecast resource of an offerer — the
     * data behind its panel. Resources that were never forecast are left out,
     * since there is no demand to compare their stock against.
     *
     * @param offererId the authenticated offerer id
     * @param atRiskOnly      whether to keep only the resources above the threshold
     * @return the assessed resources, the riskiest first
     */
    public List<WasteRiskDTO> findAllForOfferer(Long offererId, boolean atRiskOnly) {
        return forecastRepository.findLatestByOfferer(offererId).stream()
                .map(forecast -> toRisk(forecast.getResource(), forecast))
                .filter(risk -> !atRiskOnly || risk.atRisk())
                .sorted((first, second) -> second.riskPercentage().compareTo(first.riskPercentage()))
                .toList();
    }

    /**
     * Returns the risk percentage above which a resource is flagged.
     *
     * @return the configured threshold
     */
    public Percentage getThreshold() {
        return threshold;
    }

    private WasteRiskDTO toRisk(PerishableResource resource, DemandForecast forecast) {
        int stockQuantity = resource.getStockQuantity();
        int predictedQuantity = forecast.getPredictedQuantity();

        int surplus = riskCalculator.expectedSurplus(stockQuantity, predictedQuantity);
        Percentage risk = riskCalculator.calculate(stockQuantity, predictedQuantity);
        boolean atRisk = risk.value().compareTo(threshold.value()) > 0;

        return forecastMapper.toRiskResponse(resource, forecast, surplus, risk, threshold, atRisk);
    }

    private PerishableResource findResourceOwnedBy(Long id, Long offererId) {
        PerishableResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(PerishableResource.class, id));

        if (!resource.getOfferer().getId().equals(offererId)) {
            throw new EntityNotFoundException(PerishableResource.class, id);
        }
        return resource;
    }
}
