package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.aggregator.IndicatorsAggregator;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.domain.dtos.PeriodDTO;
import br.com.seudominio.foodrescue.domain.dtos.WasteAndSavingsIndicatorsDTO;
import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.enums.IndicatorPeriod;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.persistence.repositories.AiRecommendationRepository;
import br.com.seudominio.foodrescue.persistence.repositories.SaleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service that consolidates impact and waste-reduction indicators for an establishment (UC12).
 *
 * <p>Unlike entity CRUD services, this service does not extend {@code GenericService}
 * because it coordinates multiple aggregates (sales and recommendations) to produce an analytical view.</p>
 *
 * @author Clovis Luan
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class IndicatorsService {

    private final SaleRepository saleRepository;
    private final AiRecommendationRepository recommendationRepository;
    private final IndicatorsAggregator aggregator;
    private final TimeProvider timeProvider;

    /**
     * Constructor.
     *
     * @param saleRepository           the sale repository
     * @param recommendationRepository the AI recommendation repository
     * @param aggregator               the indicators aggregator
     * @param timeProvider             the current date and time provider
     */
    public IndicatorsService(
            SaleRepository saleRepository,
            AiRecommendationRepository recommendationRepository,
            IndicatorsAggregator aggregator,
            TimeProvider timeProvider) {
        this.saleRepository = saleRepository;
        this.recommendationRepository = recommendationRepository;
        this.aggregator = aggregator;
        this.timeProvider = timeProvider;
    }

    /**
     * Retrieves the consolidated indicators for an establishment within a period,
     * optionally including comparison with the immediately preceding period.
     *
     * @param establishmentId the authenticated establishment identifier
     * @param period          the predefined period type (optional, defaults to {@link IndicatorPeriod#MONTH})
     * @param startDate       custom start date (optional)
     * @param endDate         custom end date (optional)
     * @param compare         whether to calculate comparison metrics for the previous period
     * @return the consolidated indicators DTO
     * @throws BusinessRuleViolationException if {@code startDate} is after {@code endDate}
     */
    public WasteAndSavingsIndicatorsDTO getIndicators(
            Long establishmentId,
            IndicatorPeriod period,
            LocalDate startDate,
            LocalDate endDate,
            boolean compare) {

        LocalDate today = timeProvider.now().toLocalDate();
        PeriodDTO currentPeriod = resolvePeriod(period, startDate, endDate, today);

        LocalDateTime startDateTime = currentPeriod.startDate().atStartOfDay();
        LocalDateTime endDateTime = currentPeriod.endDate().plusDays(1).atStartOfDay();

        List<Sale> sales = saleRepository.findByEstablishmentAndPeriod(establishmentId, startDateTime, endDateTime);
        List<AiRecommendation> recommendations = recommendationRepository
                .findRespondedByEstablishmentAndPeriod(establishmentId, startDateTime, endDateTime);

        WasteAndSavingsIndicatorsDTO comparisonDTO = null;
        if (compare) {
            PeriodDTO prevPeriod = resolvePreviousPeriod(currentPeriod);
            LocalDateTime prevStartDateTime = prevPeriod.startDate().atStartOfDay();
            LocalDateTime prevEndDateTime = prevPeriod.endDate().plusDays(1).atStartOfDay();

            List<Sale> prevSales = saleRepository
                    .findByEstablishmentAndPeriod(establishmentId, prevStartDateTime, prevEndDateTime);
            List<AiRecommendation> prevRecs = recommendationRepository
                    .findRespondedByEstablishmentAndPeriod(establishmentId, prevStartDateTime, prevEndDateTime);

            comparisonDTO = aggregator.aggregate(prevPeriod, prevSales, prevRecs);
        }

        return aggregator.aggregate(currentPeriod, sales, recommendations, comparisonDTO);
    }

    private PeriodDTO resolvePeriod(IndicatorPeriod period, LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new BusinessRuleViolationException("startDate must not be after endDate");
            }
            return new PeriodDTO(startDate, endDate);
        }

        if (startDate != null) {
            if (startDate.isAfter(today)) {
                throw new BusinessRuleViolationException("startDate must not be after endDate");
            }
            return new PeriodDTO(startDate, today);
        }

        if (endDate != null) {
            LocalDate defaultStart = endDate.minusDays(29);
            return new PeriodDTO(defaultStart, endDate);
        }

        IndicatorPeriod resolved = (period != null) ? period : IndicatorPeriod.MONTH;
        return switch (resolved) {
            case DAY -> new PeriodDTO(today, today);
            case WEEK -> new PeriodDTO(today.minusDays(6), today);
            case MONTH, CUSTOM -> new PeriodDTO(today.withDayOfMonth(1), today);
        };
    }

    private PeriodDTO resolvePreviousPeriod(PeriodDTO currentPeriod) {
        long days = ChronoUnit.DAYS.between(currentPeriod.startDate(), currentPeriod.endDate()) + 1;
        LocalDate prevEndDate = currentPeriod.startDate().minusDays(1);
        LocalDate prevStartDate = prevEndDate.minusDays(days - 1);
        return new PeriodDTO(prevStartDate, prevEndDate);
    }
}
