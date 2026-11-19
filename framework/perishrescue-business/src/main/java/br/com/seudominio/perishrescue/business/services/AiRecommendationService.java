package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.event.DiscountRecommendationRespondedEvent;
import br.com.seudominio.perishrescue.business.event.DomainEventPublisher;
import br.com.seudominio.perishrescue.business.strategy.DiscountContext;
import br.com.seudominio.perishrescue.business.strategy.DiscountRecommendationStrategy;
import br.com.seudominio.perishrescue.business.strategy.SurplusAction;
import br.com.seudominio.perishrescue.business.strategy.SurplusContext;
import br.com.seudominio.perishrescue.business.strategy.SurplusDecision;
import br.com.seudominio.perishrescue.business.strategy.SurplusDestinationStrategy;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.perishrescue.domain.dtos.RespondToDiscountRecommendationRequest;
import br.com.seudominio.perishrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.perishrescue.domain.entities.AiRecommendation;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.RecommendationDecision;
import br.com.seudominio.perishrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.AiRecommendationMapper;
import br.com.seudominio.perishrescue.persistence.repositories.AiRecommendationRepository;

import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service that recommends a dynamic price for resources at risk of being wasted
 * and processes the offerer's answer (UC07).
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
    private final ResourceService resourceService;
    private final WasteRiskService wasteRiskService;
    private final DiscountRecommendationStrategy discountStrategy;
    private final DomainEventPublisher eventPublisher;
    private final TimeProvider timeProvider;
    private final LocalTime closingTime;
    private final long ttlHours;
    private final SurplusDestinationStrategy surplusStrategy;
    private final long reassessmentWindowHours;
    private final long reassessmentCooldownMinutes;

    /**
     * Constructor.
     *
     * @param repository       the AI recommendation repository
     * @param mapper           the AI recommendation mapper
     * @param validator        the bean validator
     * @param messageUtils     the message utils
     * @param resourceService   the resource service, which owns resource lookup and price changes
     * @param wasteRiskService the waste risk service (UC06), the precondition of this use case
     * @param discountStrategy the discount formula
     * @param eventPublisher   the domain event publisher
     * @param timeProvider     the current date and time provider
     * @param closingTime      the end of the business day, shared with the forecast of UC05
     * @param ttlHours         how many hours a pending recommendation waits before expiring
     * @param surplusStrategy  the decision between a deeper discount and priority (UC10)
     * @param reassessmentWindowHours how many hours before closing a persisting risk is reassessed (UC10)
     * @param reassessmentCooldownMinutes minimum minutes between two recommendations of the same resource (UC10)
     */
    public AiRecommendationService(
            AiRecommendationRepository repository,
            AiRecommendationMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ResourceService resourceService,
            WasteRiskService wasteRiskService,
            DiscountRecommendationStrategy discountStrategy,
            DomainEventPublisher eventPublisher,
            TimeProvider timeProvider,
            @Value("${app.forecast.closing-time:22:00}") String closingTime,
            @Value("${app.discount.recommendation-ttl-hours:24}") long ttlHours,
            SurplusDestinationStrategy surplusStrategy,
            @Value("${app.surplus.window-hours:3}") long reassessmentWindowHours,
            @Value("${app.surplus.cooldown-minutes:60}") long reassessmentCooldownMinutes) {
        super(repository, mapper, validator, messageUtils);
        this.recommendationRepository = repository;
        this.recommendationMapper = mapper;
        this.resourceService = resourceService;
        this.wasteRiskService = wasteRiskService;
        this.discountStrategy = discountStrategy;
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
        this.closingTime = LocalTime.parse(closingTime);
        this.ttlHours = ttlHours;
        this.surplusStrategy = surplusStrategy;
        this.reassessmentWindowHours = reassessmentWindowHours;
        this.reassessmentCooldownMinutes = reassessmentCooldownMinutes;
    }

    /**
     * Generates a pending discount recommendation for a resource currently
     * flagged as at risk of being wasted (UC06).
     *
     * @param resourceId       the resource id
     * @param offererId the authenticated offerer id
     * @return the recorded recommendation, with status {@code PENDING}
     * @throws EntityNotFoundException        if the resource does not exist, belongs to another
     *                                        offerer or was never forecast
     * @throws BusinessRuleViolationException if the resource is not at risk, or already has a
     *                                        pending recommendation
     */
    public DiscountRecommendationDTO recommendDiscount(Long resourceId, Long offererId) {
        PerishableResource resource = resourceService.findResourceOwnedBy(resourceId, offererId);
        WasteRiskDTO risk = wasteRiskService.assess(resourceId, offererId);

        if (!risk.atRisk()) {
            throw new BusinessRuleViolationException(String.format(
                    "resource %d is not at risk of waste: risk is %s%%, threshold is %s%%",
                    resourceId, risk.riskPercentage(), risk.riskThreshold()));
        }

        LocalDateTime now = timeProvider.now();
        AiRecommendation pending = recommendationRepository
                .findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                        resourceId, RecommendationStatus.PENDING)
                .orElse(null);

        if (pending != null && !expireIfStale(pending, now)) {
            throw new BusinessRuleViolationException(String.format(
                    "resource %d already has a pending discount recommendation (id %d)",
                    resourceId, pending.getId()));
        }

        Percentage suggested = discountStrategy.suggest(contextFor(resource, risk, now));

        AiRecommendation recommendation = AiRecommendation.builder()
                .resource(resource)
                .type(RecommendationType.DISCOUNT)
                .suggestedPercentage(suggested)
                .build();

        return toDto(recommendationRepository.save(recommendation));
    }

    /**
     * Processes the offerer's answer to a pending recommendation: accept
     * the suggested percentage, adjust it, or refuse.
     *
     * <p>Accepting or adjusting updates the resource's current price; refusing
     * leaves it untouched. Every outcome publishes a
     * {@link DiscountRecommendationRespondedEvent}.</p>
     *
     * @param recommendationId the recommendation id
     * @param offererId  the authenticated offerer id
     * @param request          the offerer's answer
     * @return the answered recommendation
     * @throws EntityNotFoundException        if the recommendation does not exist or belongs to
     *                                        another offerer
     * @throws BusinessRuleViolationException if the recommendation already expired, was already
     *                                        answered, or an adjustment came without a percentage
     */
    public DiscountRecommendationDTO respondToRecommendation(
            Long recommendationId, Long offererId, RespondToDiscountRecommendationRequest request) {
        if (request == null || request.decision() == null) {
            throw new BusinessRuleViolationException("decision must be informed");
        }

        AiRecommendation recommendation = findOwnedBy(recommendationId, offererId);
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
            resourceService.applyDiscount(recommendation.getResource().getId(), offererId, applied);
        }

        AiRecommendation answered = recommendationRepository.save(recommendation);

        eventPublisher.publish(new DiscountRecommendationRespondedEvent(
                answered.getId(),
                answered.getResource().getId(),
                offererId,
                answered.getStatus(),
                applied == null ? null : applied.value(),
                answered.getType(),
                now));

        return toDto(answered);
    }

    /**
     * Lists the recommendations of an offerer still waiting for an answer,
     * oldest first. Recommendations whose deadline has passed are marked as
     * expired and left out.
     *
     * @param offererId the authenticated offerer id
     * @return the pending recommendations
     */
    public List<DiscountRecommendationDTO> findPendingForOfferer(Long offererId) {
        LocalDateTime now = timeProvider.now();

        return recommendationRepository
                .findByResourceOffererIdAndStatusAndActiveTrueOrderByCreationDateAsc(
                        offererId, RecommendationStatus.PENDING)
                .stream()
                .filter(recommendation -> !expireIfStale(recommendation, now))
                .map(this::toDto)
                .toList();
    }

    /**
     * Lists every recommendation ever made for a resource, newest first &mdash;
     * the accepted, adjusted, refused and expired ones.
     *
     * @param resourceId       the resource id
     * @param offererId the authenticated offerer id
     * @return the resource's recommendation history
     * @throws EntityNotFoundException if the resource does not exist or belongs to another offerer
     */
    @Transactional(readOnly = true)
    public List<DiscountRecommendationDTO> findHistoryForResource(Long resourceId, Long offererId) {
        resourceService.findResourceOwnedBy(resourceId, offererId);

        return recommendationRepository.findByResourceIdAndActiveTrueOrderByCreationDateDesc(resourceId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Reassesses the risk of the resources whose last recommendation of the day
     * did not solve the problem and, when it persists close to closing time,
     * generates a new pending recommendation chained to the previous one
     * (UC10). Resources already sold out, no longer at risk, already waiting
     * for an answer or recommended too recently are left alone.
     *
     * <p>The risk is the one UC06 computes ({@link WasteRiskService}); what to
     * recommend is decided by the {@link SurplusDestinationStrategy}.</p>
     *
     * @return the recommendations generated by this run
     */
    public List<DiscountRecommendationDTO> reassessSurplusDestination() {
        LocalDateTime now = timeProvider.now();
        Duration remaining = Duration.between(now, now.toLocalDate().atTime(closingTime));

        if (remaining.isNegative() || remaining.toHours() >= reassessmentWindowHours) {
            return List.of();
        }

        return recommendationRepository.findChainTailsSince(now.toLocalDate().atStartOfDay()).stream()
                .map(tail -> reassess(tail, now, remaining))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Lists the chain of recommendations made for a resource today, in the
     * order they were generated: the initial one, then each new one pointing
     * to its predecessor through {@code previousRecommendationId} (UC10).
     *
     * @param resourceId       the resource id
     * @param offererId the authenticated offerer id
     * @return the resource's recommendations of the day, oldest first
     * @throws EntityNotFoundException if the resource does not exist or belongs to another offerer
     */
    @Transactional(readOnly = true)
    public List<DiscountRecommendationDTO> findChainForResource(Long resourceId, Long offererId) {
        resourceService.findResourceOwnedBy(resourceId, offererId);

        return recommendationRepository
                .findByResourceIdAndCreationDateGreaterThanEqualAndActiveTrueOrderByCreationDateAsc(
                        resourceId, timeProvider.now().toLocalDate().atStartOfDay())
                .stream()
                .map(this::toDto)
                .toList();
    }

    private Optional<DiscountRecommendationDTO> reassess(
            AiRecommendation tail, LocalDateTime now, Duration remaining) {
        PerishableResource resource = tail.getResource();
        Long resourceId = resource.getId();

        boolean soldOut = resource.getStockQuantity() <= 0;
        boolean tooRecent = tail.getCreationDate() == null
                || tail.getCreationDate().plusMinutes(reassessmentCooldownMinutes).isAfter(now);
        if (soldOut || tooRecent || hasPending(resourceId)) {
            return Optional.empty();
        }

        WasteRiskDTO risk;
        try {
            risk = wasteRiskService.assess(resourceId, resource.getOfferer().getId());
        } catch (EntityNotFoundException e) {
            return Optional.empty();
        }
        if (!risk.atRisk()) {
            return Optional.empty();
        }

        SurplusDecision decision = surplusStrategy.decide(new SurplusContext(
                Percentage.of(risk.riskPercentage()),
                risk.stockQuantity(),
                risk.expectedSurplus(),
                remaining.toHours(),
                currentDiscountOf(resource),
                tail.getType()));

        boolean discount = decision.action() == SurplusAction.ADDITIONAL_DISCOUNT;
        AiRecommendation next = AiRecommendation.builder()
                .resource(resource)
                .type(discount ? RecommendationType.DISCOUNT : RecommendationType.SURPLUS_DESTINATION)
                .suggestedPercentage(discount ? decision.percentage() : null)
                .previousRecommendationId(tail.getId())
                .build();

        return Optional.of(toDto(recommendationRepository.save(next)));
    }

    private boolean hasPending(Long resourceId) {
        return recommendationRepository
                .findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                        resourceId, RecommendationStatus.PENDING)
                .isPresent();
    }

    private Percentage currentDiscountOf(PerishableResource resource) {
        Money original = resource.getOriginalPrice();
        Money current = resource.getCurrentPrice();
        if (original == null || current == null || original.amount().signum() <= 0) {
            return Percentage.of(BigDecimal.ZERO);
        }

        BigDecimal discount = BigDecimal.ONE
                .subtract(current.amount().divide(original.amount(), 6, RoundingMode.HALF_UP))
                .multiply(BigDecimal.valueOf(100))
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
        return Percentage.of(discount);
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

    private DiscountContext contextFor(PerishableResource resource, WasteRiskDTO risk, LocalDateTime now) {
        LocalDateTime closingAt = now.toLocalDate().atTime(closingTime);
        LocalDate expirationDate = resource.getExpirationDate();

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

    private AiRecommendation findOwnedBy(Long id, Long offererId) {
        AiRecommendation recommendation = recommendationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(AiRecommendation.class, id));

        if (!recommendation.getResource().getOfferer().getId().equals(offererId)) {
            throw new EntityNotFoundException(AiRecommendation.class, id);
        }
        return recommendation;
    }
}
