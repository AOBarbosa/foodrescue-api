package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.risk.RiskCalculator;
import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.foodrescue.domain.entities.DemandForecast;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.DemandForecastMapper;
import br.com.seudominio.foodrescue.persistence.repositories.DemandForecastRepository;
import br.com.seudominio.foodrescue.persistence.repositories.ProductRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that identifies products at risk of being wasted (UC06).
 *
 * <p>It only combines an already existing demand forecast (produced by UC05)
 * with the product's current stock — it never recalculates the forecast, nor
 * decides on a discount (that is UC07). The formula itself lives in a
 * {@link RiskCalculator}, and the flagging threshold is read from
 * {@code app.waste-risk.threshold}, so neither can change this class.</p>
 *
 * <p>The assessment is derived on read from the product's <em>current</em>
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

    private final ProductRepository productRepository;
    private final DemandForecastRepository forecastRepository;
    private final DemandForecastMapper forecastMapper;
    private final RiskCalculator riskCalculator;
    private final Percentage threshold;

    /**
     * Constructor.
     *
     * @param productRepository  the product repository
     * @param forecastRepository the demand forecast repository
     * @param forecastMapper     the demand forecast mapper
     * @param riskCalculator     the waste risk formula
     * @param threshold          the risk percentage above which a product is flagged
     */
    public WasteRiskService(
            ProductRepository productRepository,
            DemandForecastRepository forecastRepository,
            DemandForecastMapper forecastMapper,
            RiskCalculator riskCalculator,
            @Value("${app.waste-risk.threshold:70}") double threshold) {
        this.productRepository = productRepository;
        this.forecastRepository = forecastRepository;
        this.forecastMapper = forecastMapper;
        this.riskCalculator = riskCalculator;
        this.threshold = Percentage.of(threshold);
    }

    /**
     * Assesses the waste risk of a single product from its latest forecast.
     *
     * @param productId       the product id
     * @param establishmentId the authenticated establishment id
     * @return the product's waste risk
     * @throws EntityNotFoundException if the product does not exist, belongs to another
     *                                 establishment or was never forecast
     */
    public WasteRiskDTO assess(Long productId, Long establishmentId) {
        Product product = findProductOwnedBy(productId, establishmentId);
        DemandForecast forecast = forecastRepository
                .findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(productId)
                .orElseThrow(() -> new EntityNotFoundException(DemandForecast.class, "product " + productId));

        return toRisk(product, forecast);
    }

    /**
     * Lists the waste risk of every forecast product of an establishment — the
     * data behind its panel. Products that were never forecast are left out,
     * since there is no demand to compare their stock against.
     *
     * @param establishmentId the authenticated establishment id
     * @param atRiskOnly      whether to keep only the products above the threshold
     * @return the assessed products, the riskiest first
     */
    public List<WasteRiskDTO> findAllForEstablishment(Long establishmentId, boolean atRiskOnly) {
        return forecastRepository.findLatestByEstablishment(establishmentId).stream()
                .map(forecast -> toRisk(forecast.getProduct(), forecast))
                .filter(risk -> !atRiskOnly || risk.atRisk())
                .sorted((first, second) -> second.riskPercentage().compareTo(first.riskPercentage()))
                .toList();
    }

    /**
     * Returns the risk percentage above which a product is flagged.
     *
     * @return the configured threshold
     */
    public Percentage getThreshold() {
        return threshold;
    }

    private WasteRiskDTO toRisk(Product product, DemandForecast forecast) {
        int stockQuantity = product.getStockQuantity();
        int predictedQuantity = forecast.getPredictedQuantity();

        int surplus = riskCalculator.expectedSurplus(stockQuantity, predictedQuantity);
        Percentage risk = riskCalculator.calculate(stockQuantity, predictedQuantity);
        boolean atRisk = risk.value().compareTo(threshold.value()) > 0;

        return forecastMapper.toRiskResponse(product, forecast, surplus, risk, threshold, atRisk);
    }

    private Product findProductOwnedBy(Long id, Long establishmentId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Product.class, id));

        if (!product.getEstablishment().getId().equals(establishmentId)) {
            throw new EntityNotFoundException(Product.class, id);
        }
        return product;
    }
}
