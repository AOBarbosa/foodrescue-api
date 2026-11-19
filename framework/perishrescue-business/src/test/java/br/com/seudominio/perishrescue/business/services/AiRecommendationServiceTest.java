package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.event.DiscountRecommendationRespondedEvent;
import br.com.seudominio.perishrescue.business.event.DomainEventPublisher;
import br.com.seudominio.perishrescue.business.strategy.DiscountThenPriorityStrategy;
import br.com.seudominio.perishrescue.business.strategy.RiskWeightedDiscountStrategy;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.perishrescue.domain.dtos.RespondToDiscountRecommendationRequest;
import br.com.seudominio.perishrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.perishrescue.domain.entities.AiRecommendation;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.RecommendationDecision;
import br.com.seudominio.perishrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.AiRecommendationMapper;
import br.com.seudominio.perishrescue.persistence.repositories.AiRecommendationRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class AiRecommendationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);

    private AiRecommendationRepository recommendationRepository;
    private ResourceService resourceService;
    private WasteRiskService wasteRiskService;
    private DomainEventPublisher eventPublisher;
    private AiRecommendationService service;
    private Offerer offerer;
    private PerishableResource resource;

    @BeforeEach
    void setUp() {
        recommendationRepository = mock(AiRecommendationRepository.class);
        resourceService = mock(ResourceService.class);
        wasteRiskService = mock(WasteRiskService.class);
        eventPublisher = mock(DomainEventPublisher.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        when(recommendationRepository.save(any(AiRecommendation.class))).thenAnswer(invocation -> {
            AiRecommendation saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(500L);
                saved.setCreationDate(NOW);
            }
            return saved;
        });

        service = new AiRecommendationService(
                recommendationRepository, new AiRecommendationMapper(), mock(Validator.class),
                mock(MessageUtils.class), resourceService, wasteRiskService,
                new RiskWeightedDiscountStrategy(0.6, 15, 4, 60), eventPublisher, timeProvider, "22:00", 24,
                new DiscountThenPriorityStrategy(10, 60), 3, 60);

        offerer = new Offerer();
        offerer.setId(1L);
        resource = new TestResource();
        resource.setId(10L);
        resource.setName("Pao frances");
        resource.setOfferer(offerer);
        resource.setStockQuantity(20);
        resource.setOriginalPrice(Money.of(10.00));
        resource.setCurrentPrice(Money.of(10.00));

        when(resourceService.findResourceOwnedBy(10L, 1L)).thenReturn(resource);
    }

    private WasteRiskDTO risk(boolean atRisk, double percentage, int surplus) {
        return new WasteRiskDTO(10L, "Pao frances", 20, 20 - surplus, surplus,
                BigDecimal.valueOf(percentage), BigDecimal.valueOf(70), atRisk, 99L, NOW);
    }

    private AiRecommendation pending(Long id, Percentage suggested, LocalDateTime createdAt) {
        AiRecommendation recommendation = AiRecommendation.builder()
                .id(id)
                .resource(resource)
                .type(RecommendationType.DISCOUNT)
                .suggestedPercentage(suggested)
                .build();
        recommendation.setCreationDate(createdAt);
        return recommendation;
    }

    // ---------- recommendDiscount ----------

    @Test
    void recommendDiscountCreatesPendingRecommendationForResourceAtRisk() {
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(true, 80, 16));
        when(recommendationRepository.findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING)).thenReturn(Optional.empty());

        DiscountRecommendationDTO dto = service.recommendDiscount(10L, 1L);

        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.resourceId()).isEqualTo(10L);
        assertThat(dto.type()).isEqualTo(RecommendationType.DISCOUNT);
        assertThat(dto.status()).isEqualTo(RecommendationStatus.PENDING);
        assertThat(dto.suggestedPercentage()).isEqualByComparingTo("48.00");
        assertThat(dto.originalPrice()).isEqualByComparingTo("10.00");
        assertThat(dto.priceWithDiscount()).isEqualByComparingTo("5.20");
        assertThat(dto.expiresAt()).isEqualTo(NOW.plusHours(24));
        assertThat(dto.respondedAt()).isNull();
    }

    @Test
    void recommendDiscountRaisesTheSuggestionWhenTheResourceExpiresTomorrow() {
        resource.setExpirationDate(LocalDate.of(2026, 9, 15));
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(true, 80, 16));
        when(recommendationRepository.findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING)).thenReturn(Optional.empty());

        assertThat(service.recommendDiscount(10L, 1L).suggestedPercentage()).isEqualByComparingTo("60.00");
    }

    @Test
    void recommendDiscountRejectsResourceThatIsNotAtRisk() {
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(false, 20, 4));

        assertThatThrownBy(() -> service.recommendDiscount(10L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("is not at risk of waste");
        verify(recommendationRepository, never()).save(any(AiRecommendation.class));
    }

    @Test
    void recommendDiscountRejectsASecondPendingRecommendationForTheSameResource() {
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(true, 80, 16));
        when(recommendationRepository.findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING))
                .thenReturn(Optional.of(pending(400L, Percentage.of(30), NOW.minusHours(1))));

        assertThatThrownBy(() -> service.recommendDiscount(10L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already has a pending discount recommendation");
    }

    @Test
    void recommendDiscountReplacesAPendingRecommendationThatAlreadyExpired() {
        AiRecommendation stale = pending(400L, Percentage.of(30), NOW.minusHours(25));
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(true, 80, 16));
        when(recommendationRepository.findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING)).thenReturn(Optional.of(stale));

        DiscountRecommendationDTO dto = service.recommendDiscount(10L, 1L);

        assertThat(stale.getStatus()).isEqualTo(RecommendationStatus.EXPIRED);
        assertThat(dto.status()).isEqualTo(RecommendationStatus.PENDING);
    }

    @Test
    void recommendDiscountRejectsResourceFromAnotherOfferer() {
        when(resourceService.findResourceOwnedBy(10L, 2L)).thenThrow(new EntityNotFoundException(PerishableResource.class, 10L));

        assertThatThrownBy(() -> service.recommendDiscount(10L, 2L)).isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- respondToRecommendation ----------

    @Test
    void acceptingARecommendationUpdatesTheResourcePrice() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        DiscountRecommendationDTO dto = service.respondToRecommendation(
                500L, 1L, new RespondToDiscountRecommendationRequest(RecommendationDecision.ACCEPT, null));

        assertThat(dto.status()).isEqualTo(RecommendationStatus.ACCEPTED);
        assertThat(dto.suggestedPercentage()).isEqualByComparingTo("40.00");
        assertThat(dto.respondedAt()).isEqualTo(NOW);
        verify(resourceService).applyDiscount(10L, 1L, Percentage.of(40));
    }

    @Test
    void adjustingARecommendationAppliesTheOfferersOwnPercentage() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        DiscountRecommendationDTO dto = service.respondToRecommendation(500L, 1L,
                new RespondToDiscountRecommendationRequest(
                        RecommendationDecision.ADJUST, BigDecimal.valueOf(25)));

        assertThat(dto.status()).isEqualTo(RecommendationStatus.ADJUSTED);
        assertThat(dto.suggestedPercentage()).isEqualByComparingTo("25.00");
        verify(resourceService).applyDiscount(10L, 1L, Percentage.of(25));
    }

    @Test
    void adjustingWithoutAPercentageIsRejectedAndLeavesThePriceUntouched() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        assertThatThrownBy(() -> service.respondToRecommendation(500L, 1L,
                new RespondToDiscountRecommendationRequest(RecommendationDecision.ADJUST, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("adjustedPercentage must be informed");

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.PENDING);
        verify(resourceService, never()).applyDiscount(any(), any(), any());
    }

    @Test
    void refusingARecommendationLeavesThePriceUnchanged() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        DiscountRecommendationDTO dto = service.respondToRecommendation(
                500L, 1L, new RespondToDiscountRecommendationRequest(RecommendationDecision.REFUSE, null));

        assertThat(dto.status()).isEqualTo(RecommendationStatus.REFUSED);
        assertThat(dto.currentPrice()).isEqualByComparingTo("10.00");
        verify(resourceService, never()).applyDiscount(any(), any(), any());
    }

    @Test
    void respondingToAnExpiredRecommendationIsRejectedAndMarksItExpired() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(25));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        assertThatThrownBy(() -> service.respondToRecommendation(500L, 1L,
                new RespondToDiscountRecommendationRequest(RecommendationDecision.ACCEPT, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is EXPIRED");

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.EXPIRED);
        verify(resourceService, never()).applyDiscount(any(), any(), any());
    }

    @Test
    void respondingTwiceIsRejected() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        recommendation.setStatus(RecommendationStatus.ACCEPTED);
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        assertThatThrownBy(() -> service.respondToRecommendation(500L, 1L,
                new RespondToDiscountRecommendationRequest(RecommendationDecision.REFUSE, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is ACCEPTED");
    }

    @Test
    void respondingToARecommendationOfAnotherOffererIsRejected() {
        when(recommendationRepository.findById(500L))
                .thenReturn(Optional.of(pending(500L, Percentage.of(40), NOW)));

        assertThatThrownBy(() -> service.respondToRecommendation(500L, 2L,
                new RespondToDiscountRecommendationRequest(RecommendationDecision.ACCEPT, null)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void answeringPublishesTheDomainEventForUc08() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        service.respondToRecommendation(
                500L, 1L, new RespondToDiscountRecommendationRequest(RecommendationDecision.ACCEPT, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());

        assertThat(captor.getValue()).isInstanceOf(DiscountRecommendationRespondedEvent.class);
        DiscountRecommendationRespondedEvent event = (DiscountRecommendationRespondedEvent) captor.getValue();
        assertThat(event.recommendationId()).isEqualTo(500L);
        assertThat(event.resourceId()).isEqualTo(10L);
        assertThat(event.offererId()).isEqualTo(1L);
        assertThat(event.status()).isEqualTo(RecommendationStatus.ACCEPTED);
        assertThat(event.appliedPercentage()).isEqualByComparingTo("40.00");
        assertThat(event.recommendationType()).isEqualTo(RecommendationType.DISCOUNT);
        assertThat(event.respondedAt()).isEqualTo(NOW);
    }

    @Test
    void refusalPublishesTheEventWithoutAnAppliedPercentage() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        service.respondToRecommendation(
                500L, 1L, new RespondToDiscountRecommendationRequest(RecommendationDecision.REFUSE, null));

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());

        DiscountRecommendationRespondedEvent event = (DiscountRecommendationRespondedEvent) captor.getValue();
        assertThat(event.status()).isEqualTo(RecommendationStatus.REFUSED);
        assertThat(event.appliedPercentage()).isNull();
    }

    // ---------- listagens ----------

    @Test
    void findPendingLeavesOutRecommendationsThatJustExpired() {
        AiRecommendation fresh = pending(500L, Percentage.of(40), NOW.minusHours(1));
        AiRecommendation stale = pending(501L, Percentage.of(30), NOW.minusHours(30));
        when(recommendationRepository.findByResourceOffererIdAndStatusAndActiveTrueOrderByCreationDateAsc(
                eq(1L), eq(RecommendationStatus.PENDING))).thenReturn(List.of(fresh, stale));

        List<DiscountRecommendationDTO> pendingList = service.findPendingForOfferer(1L);

        assertThat(pendingList).extracting(DiscountRecommendationDTO::id).containsExactly(500L);
        assertThat(stale.getStatus()).isEqualTo(RecommendationStatus.EXPIRED);
        verify(recommendationRepository).save(stale);
    }

    @Test
    void findHistoryReturnsEveryRecommendationOfTheResource() {
        AiRecommendation accepted = pending(500L, Percentage.of(40), NOW.minusDays(2));
        accepted.setStatus(RecommendationStatus.ACCEPTED);
        AiRecommendation refused = pending(501L, Percentage.of(30), NOW.minusDays(3));
        refused.setStatus(RecommendationStatus.REFUSED);
        when(recommendationRepository.findByResourceIdAndActiveTrueOrderByCreationDateDesc(10L))
                .thenReturn(List.of(accepted, refused));

        List<DiscountRecommendationDTO> history = service.findHistoryForResource(10L, 1L);

        assertThat(history).extracting(DiscountRecommendationDTO::id).containsExactly(500L, 501L);
        assertThat(history).extracting(DiscountRecommendationDTO::status)
                .containsExactly(RecommendationStatus.ACCEPTED, RecommendationStatus.REFUSED);
    }

    @Test
    void findHistoryRejectsResourceFromAnotherOfferer() {
        when(resourceService.findResourceOwnedBy(10L, 2L)).thenThrow(new EntityNotFoundException(PerishableResource.class, 10L));

        assertThatThrownBy(() -> service.findHistoryForResource(10L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
