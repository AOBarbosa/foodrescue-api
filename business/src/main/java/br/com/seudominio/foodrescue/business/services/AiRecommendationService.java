package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.event.DiscountRecommendationRespondedEvent;
import br.com.seudominio.foodrescue.business.event.DomainEventPublisher;
import br.com.seudominio.foodrescue.business.strategy.DiscountContext;
import br.com.seudominio.foodrescue.business.strategy.DiscountRecommendationStrategy;
import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.foodrescue.domain.dtos.RespondToDiscountRecommendationRequest;
import br.com.seudominio.foodrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.RecommendationDecision;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.AiRecommendationMapper;
import br.com.seudominio.foodrescue.persistence.repositories.AiRecommendationRepository;

import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service that recommends a dynamic price for products at risk of being wasted
 * and processes the establishment's answer (UC07).
 *
 * <p>The two responsibilities the issue separates are kept as two independent
 * entry points: {@link #recommendDiscount(Long, Long)} only produces a
 * suggestion, and {@link #respondToRecommendation(Long, Long,
 * RespondToDiscountRecommendationRequest)} only processes the answer. Neither
 * knows the discount formula &mdash; that lives behind
 * {@link DiscountRecommendationStrategy}.</p>
 *
 * <p>Answering a recommendation publishes a
 * {@link DiscountRecommendationRespondedEvent} instead of calling offer
 * creation (UC08) directly, so the two use cases stay decoupled.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Service
@Transactional
public class AiRecommendationService extends GenericService<AiRecommendation, DiscountRecommendationDTO> {

    private final AiRecommendationRepository recommendationRepository;
    private final AiRecommendationMapper recommendationMapper;
    private final ProductService productService;
    private final WasteRiskService wasteRiskService;
    private final DiscountRecommendationStrategy discountStrategy;
    private final DomainEventPublisher eventPublisher;
    private final TimeProvider timeProvider;
    private final LocalTime closingTime;
    private final long ttlHours;

    /**
     * Constructor.
     *
     * @param repository       the AI recommendation repository
     * @param mapper           the AI recommendation mapper
     * @param validator        the bean validator
     * @param messageUtils     the message utils
     * @param productService   the product service, which owns product lookup and price changes
     * @param wasteRiskService the waste risk service (UC06), the precondition of this use case
     * @param discountStrategy the discount formula
     * @param eventPublisher   the domain event publisher
     * @param timeProvider     the current date and time provider
     * @param closingTime      the end of the business day, shared with the forecast of UC05
     * @param ttlHours         how many hours a pending recommendation waits before expiring
     */
    public AiRecommendationService(
            AiRecommendationRepository repository,
            AiRecommendationMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ProductService productService,
            WasteRiskService wasteRiskService,
            DiscountRecommendationStrategy discountStrategy,
            DomainEventPublisher eventPublisher,
            TimeProvider timeProvider,
            @Value("${app.forecast.closing-time:22:00}") String closingTime,
            @Value("${app.discount.recommendation-ttl-hours:24}") long ttlHours) {
        super(repository, mapper, validator, messageUtils);
        this.recommendationRepository = repository;
        this.recommendationMapper = mapper;
        this.productService = productService;
        this.wasteRiskService = wasteRiskService;
        this.discountStrategy = discountStrategy;
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
        this.closingTime = LocalTime.parse(closingTime);
        this.ttlHours = ttlHours;
    }

    /**
     * Generates a pending discount recommendation for a product currently
     * flagged as at risk of being wasted (UC06).
     *
     * @param productId       the product id
     * @param establishmentId the authenticated establishment id
     * @return the recorded recommendation, with status {@code PENDING}
     * @throws EntityNotFoundException        if the product does not exist, belongs to another
     *                                        establishment or was never forecast
     * @throws BusinessRuleViolationException if the product is not at risk, or already has a
     *                                        pending recommendation
     */
    public DiscountRecommendationDTO recommendDiscount(Long productId, Long establishmentId) {
        Product product = productService.findProductOwnedBy(productId, establishmentId);
        WasteRiskDTO risk = wasteRiskService.assess(productId, establishmentId);

        if (!risk.atRisk()) {
            throw new BusinessRuleViolationException(String.format(
                    "product %d is not at risk of waste: risk is %s%%, threshold is %s%%",
                    productId, risk.riskPercentage(), risk.riskThreshold()));
        }

        LocalDateTime now = timeProvider.now();
        AiRecommendation pending = recommendationRepository
                .findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                        productId, RecommendationStatus.PENDING)
                .orElse(null);

        if (pending != null && !expireIfStale(pending, now)) {
            throw new BusinessRuleViolationException(String.format(
                    "product %d already has a pending discount recommendation (id %d)",
                    productId, pending.getId()));
        }

        Percentage suggested = discountStrategy.suggest(contextFor(product, risk, now));

        AiRecommendation recommendation = AiRecommendation.builder()
                .product(product)
                .type(RecommendationType.DISCOUNT)
                .suggestedPercentage(suggested)
                .build();

        return toDto(recommendationRepository.save(recommendation));
    }

    /**
     * Processes the establishment's answer to a pending recommendation: accept
     * the suggested percentage, adjust it, or refuse.
     *
     * <p>Accepting or adjusting updates the product's current price; refusing
     * leaves it untouched. Every outcome publishes a
     * {@link DiscountRecommendationRespondedEvent}.</p>
     *
     * @param recommendationId the recommendation id
     * @param establishmentId  the authenticated establishment id
     * @param request          the establishment's answer
     * @return the answered recommendation
     * @throws EntityNotFoundException        if the recommendation does not exist or belongs to
     *                                        another establishment
     * @throws BusinessRuleViolationException if the recommendation already expired, was already
     *                                        answered, or an adjustment came without a percentage
     */
    public DiscountRecommendationDTO respondToRecommendation(
            Long recommendationId, Long establishmentId, RespondToDiscountRecommendationRequest request) {
        if (request == null || request.decision() == null) {
            throw new BusinessRuleViolationException("decision must be informed");
        }

        AiRecommendation recommendation = findOwnedBy(recommendationId, establishmentId);
        LocalDateTime now = timeProvider.now();

        expireIfStale(recommendation, now);
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new BusinessRuleViolationException(String.format(
                    "recommendation %d is not pending: current status is %s",
                    recommendationId, recommendation.getStatus()));
        }

        Percentage applied = switch (request.decision()) {
            case ACCEPT -> recommendation.getSuggestedPercentage();
            case ADJUST -> adjustedPercentageOf(request);
            case REFUSE -> null;
        };

        recommendation.setStatus(statusFor(request.decision()));
        recommendation.setRespondedAt(now);
        if (applied != null) {
            recommendation.setSuggestedPercentage(applied);
            productService.applyDiscount(recommendation.getProduct().getId(), establishmentId, applied);
        }

        AiRecommendation answered = recommendationRepository.save(recommendation);

        eventPublisher.publish(new DiscountRecommendationRespondedEvent(
                answered.getId(),
                answered.getProduct().getId(),
                establishmentId,
                answered.getStatus(),
                applied == null ? null : applied.value(),
                now));

        return toDto(answered);
    }

    /**
     * Lists the recommendations of an establishment still waiting for an answer,
     * oldest first. Recommendations whose deadline has passed are marked as
     * expired and left out.
     *
     * @param establishmentId the authenticated establishment id
     * @return the pending recommendations
     */
    public List<DiscountRecommendationDTO> findPendingForEstablishment(Long establishmentId) {
        LocalDateTime now = timeProvider.now();

        return recommendationRepository
                .findByProductEstablishmentIdAndStatusAndActiveTrueOrderByCreationDateAsc(
                        establishmentId, RecommendationStatus.PENDING)
                .stream()
                .filter(recommendation -> !expireIfStale(recommendation, now))
                .map(this::toDto)
                .toList();
    }

    /**
     * Lists every recommendation ever made for a product, newest first &mdash;
     * the accepted, adjusted, refused and expired ones.
     *
     * @param productId       the product id
     * @param establishmentId the authenticated establishment id
     * @return the product's recommendation history
     * @throws EntityNotFoundException if the product does not exist or belongs to another establishment
     */
    @Transactional(readOnly = true)
    public List<DiscountRecommendationDTO> findHistoryForProduct(Long productId, Long establishmentId) {
        productService.findProductOwnedBy(productId, establishmentId);

        return recommendationRepository.findByProductIdAndActiveTrueOrderByCreationDateDesc(productId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Marks a pending recommendation as expired once its deadline has passed.
     *
     * @param recommendation the recommendation to check
     * @param now            the current moment
     * @return {@code true} if it was just expired, {@code false} if it is still answerable
     *         or was not pending to begin with
     */
    private boolean expireIfStale(AiRecommendation recommendation, LocalDateTime now) {
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            return false;
        }

        LocalDateTime expiresAt = expiresAt(recommendation);
        if (expiresAt == null || now.isBefore(expiresAt)) {
            return false;
        }

        recommendation.setStatus(RecommendationStatus.EXPIRED);
        recommendationRepository.save(recommendation);
        return true;
    }

    private DiscountContext contextFor(Product product, WasteRiskDTO risk, LocalDateTime now) {
        LocalDateTime closingAt = now.toLocalDate().atTime(closingTime);
        LocalDate expirationDate = product.getExpirationDate();

        return new DiscountContext(
                Percentage.of(risk.riskPercentage()),
                risk.stockQuantity(),
                risk.expectedSurplus(),
                Duration.between(now, closingAt).toHours(),
                expirationDate == null ? null : ChronoUnit.DAYS.between(now.toLocalDate(), expirationDate));
    }

    private LocalDateTime expiresAt(AiRecommendation recommendation) {
        LocalDateTime createdAt = recommendation.getCreationDate();
        return createdAt == null ? null : createdAt.plusHours(ttlHours);
    }

    private DiscountRecommendationDTO toDto(AiRecommendation recommendation) {
        return recommendationMapper.toDto(recommendation, expiresAt(recommendation));
    }

    private Percentage adjustedPercentageOf(RespondToDiscountRecommendationRequest request) {
        if (request.adjustedPercentage() == null) {
            throw new BusinessRuleViolationException("adjustedPercentage must be informed to adjust a recommendation");
        }
        return Percentage.of(request.adjustedPercentage());
    }

    private RecommendationStatus statusFor(RecommendationDecision decision) {
        return switch (decision) {
            case ACCEPT -> RecommendationStatus.ACCEPTED;
            case ADJUST -> RecommendationStatus.ADJUSTED;
            case REFUSE -> RecommendationStatus.REFUSED;
        };
    }

    private AiRecommendation findOwnedBy(Long id, Long establishmentId) {
        AiRecommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AiRecommendation.class, id));

        if (!recommendation.getProduct().getEstablishment().getId().equals(establishmentId)) {
            throw new EntityNotFoundException(AiRecommendation.class, id);
        }
        return recommendation;
    }
}
