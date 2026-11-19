package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.forecast.DemandForecastStrategy;
import br.com.seudominio.perishrescue.business.forecast.ForecastContext;
import br.com.seudominio.perishrescue.business.forecast.ForecastEligibilityChecker;
import br.com.seudominio.perishrescue.business.forecast.ForecastResult;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.domain.dtos.DemandForecastResponse;
import br.com.seudominio.perishrescue.domain.entities.DemandForecast;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.entities.Sale;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.DemandForecastMapper;
import br.com.seudominio.perishrescue.persistence.repositories.DemandForecastRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ResourceRepository;
import br.com.seudominio.perishrescue.persistence.repositories.SaleRepository;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service that provides demand forecasting for resources (UC05).
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
    private final ResourceRepository resourceRepository;
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
     * @param resourceRepository   the resource repository
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
            ResourceRepository resourceRepository,
            SaleRepository saleRepository,
            DemandForecastStrategy forecastStrategy,
            ForecastEligibilityChecker eligibilityChecker,
            TimeProvider timeProvider,
            @Value("${app.forecast.closing-time:22:00}") String closingTime,
            @Value("${app.forecast.lookback-weeks:4}") int lookbackWeeks) {
        super(repository, mapper, validator, messageUtils);
        this.forecastRepository = repository;
        this.forecastMapper = mapper;
        this.resourceRepository = resourceRepository;
        this.saleRepository = saleRepository;
        this.forecastStrategy = forecastStrategy;
        this.eligibilityChecker = eligibilityChecker;
        this.timeProvider = timeProvider;
        this.closingTime = LocalTime.parse(closingTime);
        this.lookbackWeeks = lookbackWeeks;
    }

    /**
     * Calculates and records the demand forecast of a resource until the end
     * of the current business day.
     *
     * @param resourceId       the resource id
     * @param offererId the authenticated offerer id
     * @return the recorded forecast
     * @throws EntityNotFoundException if the resource does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if the resource lacks the minimum sales history
     */
    public DemandForecastResponse predictDemand(Long resourceId, Long offererId) {
        PerishableResource resource = findResourceOwnedBy(resourceId, offererId);
        LocalDateTime now = timeProvider.now();

        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        List<Sale> salesHistory = saleRepository.findByResourceAndPeriod(
                resourceId, startOfToday.minusWeeks(lookbackWeeks), startOfToday);

        if (!eligibilityChecker.isEligible(salesHistory)) {
            throw new BusinessRuleViolationException(String.format(
                    "insufficient sales history to forecast demand: %d sales found in the last %d weeks, "
                            + "at least %d required",
                    salesHistory.size(), lookbackWeeks, eligibilityChecker.getMinimumSales()));
        }

        LocalDateTime closingAt = now.toLocalDate().atTime(closingTime);
        ForecastContext context = ForecastContext.builder()
                .resourceName(resource.getName())
                .resourceCategory(resource.getCategory())
                .salesHistory(salesHistory)
                .currentStock(resource.getStockQuantity())
                .now(now)
                .closingAt(closingAt)
                .build();
        ForecastResult result = forecastStrategy.forecast(context);

        DemandForecast forecast = DemandForecast.builder()
                .resource(resource)
                .predictedQuantity(result.predictedQuantity())
                .stockQuantity(resource.getStockQuantity())
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
     * Finds the most recent demand forecast of a resource.
     *
     * @param resourceId       the resource id
     * @param offererId the authenticated offerer id
     * @return the latest forecast
     * @throws EntityNotFoundException if the resource does not exist, belongs to another
     *                                 offerer or was never forecast
     */
    @Transactional(readOnly = true)
    public DemandForecastResponse findLatest(Long resourceId, Long offererId) {
        findResourceOwnedBy(resourceId, offererId);

        return forecastRepository.findFirstByResourceIdAndActiveTrueOrderByCalculatedAtDesc(resourceId)
                .map(forecastMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException(DemandForecast.class, "resource " + resourceId));
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
