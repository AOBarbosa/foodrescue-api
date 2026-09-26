package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.forecast.DemandForecastStrategy;
import br.com.seudominio.foodrescue.business.forecast.ForecastContext;
import br.com.seudominio.foodrescue.business.forecast.ForecastEligibilityChecker;
import br.com.seudominio.foodrescue.business.forecast.ForecastResult;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.foodrescue.domain.entities.DemandForecast;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.DemandForecastMapper;
import br.com.seudominio.foodrescue.persistence.repositories.DemandForecastRepository;
import br.com.seudominio.foodrescue.persistence.repositories.ProductRepository;
import br.com.seudominio.foodrescue.persistence.repositories.SaleRepository;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service that provides demand forecasting for products (UC05).
 *
 * <p>Collecting the sales history ({@link SaleRepository}), calculating the
 * forecast ({@link DemandForecastStrategy}) and persisting the result
 * ({@link DemandForecastRepository}) are kept in separate collaborators; this
 * service only orchestrates them.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
@Transactional
public class DemandForecastService extends GenericService<DemandForecast, DemandForecastResponse> {

    private final DemandForecastRepository forecastRepository;
    private final DemandForecastMapper forecastMapper;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final DemandForecastStrategy forecastStrategy;
    private final ForecastEligibilityChecker eligibilityChecker;
    private final TimeProvider timeProvider;
    private final LocalTime closingTime;
    private final int lookbackWeeks;

    /**
     * Constructor.
     *
     * @param repository          the demand forecast repository
     * @param mapper              the demand forecast mapper
     * @param validator           the bean validator
     * @param messageUtils        the message utils
     * @param productRepository   the product repository
     * @param saleRepository      the sale repository
     * @param forecastStrategy    the forecasting algorithm
     * @param eligibilityChecker  the minimum-history rule
     * @param timeProvider        the current date and time provider
     * @param closingTime         the end of the business day (ISO time, e.g. {@code 22:00})
     * @param lookbackWeeks       how many weeks of sales history are considered
     */
    public DemandForecastService(
            DemandForecastRepository repository,
            DemandForecastMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ProductRepository productRepository,
            SaleRepository saleRepository,
            DemandForecastStrategy forecastStrategy,
            ForecastEligibilityChecker eligibilityChecker,
            TimeProvider timeProvider,
            @Value("${app.forecast.closing-time:22:00}") String closingTime,
            @Value("${app.forecast.lookback-weeks:4}") int lookbackWeeks) {
        super(repository, mapper, validator, messageUtils);
        this.forecastRepository = repository;
        this.forecastMapper = mapper;
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.forecastStrategy = forecastStrategy;
        this.eligibilityChecker = eligibilityChecker;
        this.timeProvider = timeProvider;
        this.closingTime = LocalTime.parse(closingTime);
        this.lookbackWeeks = lookbackWeeks;
    }

    /**
     * Calculates and records the demand forecast of a product until the end
     * of the current business day.
     *
     * @param productId       the product id
     * @param establishmentId the authenticated establishment id
     * @return the recorded forecast
     * @throws EntityNotFoundException if the product does not exist or belongs to another establishment
     * @throws BusinessRuleViolationException if the product lacks the minimum sales history
     */
    public DemandForecastResponse predictDemand(Long productId, Long establishmentId) {
        Product product = findProductOwnedBy(productId, establishmentId);
        LocalDateTime now = timeProvider.now();

        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        List<Sale> salesHistory = saleRepository.findByProductAndPeriod(
                productId, startOfToday.minusWeeks(lookbackWeeks), startOfToday);

        if (!eligibilityChecker.isEligible(salesHistory)) {
            throw new BusinessRuleViolationException(String.format(
                    "insufficient sales history to forecast demand: %d sales found in the last %d weeks, "
                            + "at least %d required",
                    salesHistory.size(), lookbackWeeks, eligibilityChecker.getMinimumSales()));
        }

        LocalDateTime closingAt = now.toLocalDate().atTime(closingTime);
        ForecastContext context = ForecastContext.builder()
                .productName(product.getName())
                .productCategory(product.getCategory())
                .salesHistory(salesHistory)
                .currentStock(product.getStockQuantity())
                .now(now)
                .closingAt(closingAt)
                .build();
        ForecastResult result = forecastStrategy.forecast(context);

        DemandForecast forecast = DemandForecast.builder()
                .product(product)
                .predictedQuantity(result.predictedQuantity())
                .stockQuantity(product.getStockQuantity())
                .confidence(result.confidence())
                .sampleSize(result.sampleSize())
                .source(result.source())
                .rationale(result.rationale())
                .calculatedAt(now)
                .forecastUntil(closingAt)
                .build();

        return forecastMapper.toDto(forecastRepository.save(forecast));
    }

    /**
     * Finds the most recent demand forecast of a product.
     *
     * @param productId       the product id
     * @param establishmentId the authenticated establishment id
     * @return the latest forecast
     * @throws EntityNotFoundException if the product does not exist, belongs to another
     *                                 establishment or was never forecast
     */
    @Transactional(readOnly = true)
    public DemandForecastResponse findLatest(Long productId, Long establishmentId) {
        findProductOwnedBy(productId, establishmentId);

        return forecastRepository.findFirstByProductIdAndActiveTrueOrderByCalculatedAtDesc(productId)
                .map(forecastMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException(DemandForecast.class, "product " + productId));
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
