package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.event.DiscountRecommendationRespondedEvent;
import br.com.seudominio.foodrescue.business.event.DomainEventPublisher;
import br.com.seudominio.foodrescue.business.strategy.RiskWeightedDiscountStrategy;
import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.foodrescue.domain.dtos.RespondToDiscountRecommendationRequest;
import br.com.seudominio.foodrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.RecommendationDecision;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.AiRecommendationMapper;
import br.com.seudominio.foodrescue.persistence.repositories.AiRecommendationRepository;

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
    private ProductService productService;
    private WasteRiskService wasteRiskService;
    private DomainEventPublisher eventPublisher;
    private AiRecommendationService service;
    private Establishment establishment;
    private Product product;

    @BeforeEach
    void setUp() {
        recommendationRepository = mock(AiRecommendationRepository.class);
        productService = mock(ProductService.class);
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
                mock(MessageUtils.class), productService, wasteRiskService,
                new RiskWeightedDiscountStrategy(0.6, 15, 4, 60), eventPublisher, timeProvider, "22:00", 24);

        establishment = new Establishment();
        establishment.setId(1L);
        product = new Product();
        product.setId(10L);
        product.setName("Pao frances");
        product.setEstablishment(establishment);
        product.setStockQuantity(20);
        product.setOriginalPrice(Money.of(10.00));
        product.setCurrentPrice(Money.of(10.00));

        when(productService.findProductOwnedBy(10L, 1L)).thenReturn(product);
    }

    private WasteRiskDTO risk(boolean atRisk, double percentage, int surplus) {
        return new WasteRiskDTO(10L, "Pao frances", 20, 20 - surplus, surplus,
                BigDecimal.valueOf(percentage), BigDecimal.valueOf(70), atRisk, 99L, NOW);
    }

    private AiRecommendation pending(Long id, Percentage suggested, LocalDateTime createdAt) {
        AiRecommendation recommendation = AiRecommendation.builder()
                .id(id)
                .product(product)
                .type(RecommendationType.DISCOUNT)
                .suggestedPercentage(suggested)
                .build();
        recommendation.setCreationDate(createdAt);
        return recommendation;
    }

    // ---------- recommendDiscount ----------

    @Test
    void recommendDiscountCreatesPendingRecommendationForProductAtRisk() {
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(true, 80, 16));
        when(recommendationRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING)).thenReturn(Optional.empty());

        DiscountRecommendationDTO dto = service.recommendDiscount(10L, 1L);

        assertThat(dto.id()).isEqualTo(500L);
        assertThat(dto.productId()).isEqualTo(10L);
        assertThat(dto.type()).isEqualTo(RecommendationType.DISCOUNT);
        assertThat(dto.status()).isEqualTo(RecommendationStatus.PENDING);
        assertThat(dto.suggestedPercentage()).isEqualByComparingTo("48.00");
        assertThat(dto.originalPrice()).isEqualByComparingTo("10.00");
        assertThat(dto.priceWithDiscount()).isEqualByComparingTo("5.20");
        assertThat(dto.expiresAt()).isEqualTo(NOW.plusHours(24));
        assertThat(dto.respondedAt()).isNull();
    }

    @Test
    void recommendDiscountRaisesTheSuggestionWhenTheProductExpiresTomorrow() {
        product.setExpirationDate(LocalDate.of(2026, 9, 15));
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(true, 80, 16));
        when(recommendationRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING)).thenReturn(Optional.empty());

        assertThat(service.recommendDiscount(10L, 1L).suggestedPercentage()).isEqualByComparingTo("60.00");
    }

    @Test
    void recommendDiscountRejectsProductThatIsNotAtRisk() {
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(false, 20, 4));

        assertThatThrownBy(() -> service.recommendDiscount(10L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("is not at risk of waste");
        verify(recommendationRepository, never()).save(any(AiRecommendation.class));
    }

    @Test
    void recommendDiscountRejectsASecondPendingRecommendationForTheSameProduct() {
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(true, 80, 16));
        when(recommendationRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
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
        when(recommendationRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING)).thenReturn(Optional.of(stale));

        DiscountRecommendationDTO dto = service.recommendDiscount(10L, 1L);

        assertThat(stale.getStatus()).isEqualTo(RecommendationStatus.EXPIRED);
        assertThat(dto.status()).isEqualTo(RecommendationStatus.PENDING);
    }

    @Test
    void recommendDiscountRejectsProductFromAnotherEstablishment() {
        when(productService.findProductOwnedBy(10L, 2L)).thenThrow(new EntityNotFoundException(Product.class, 10L));

        assertThatThrownBy(() -> service.recommendDiscount(10L, 2L)).isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- respondToRecommendation ----------

    @Test
    void acceptingARecommendationUpdatesTheProductPrice() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        DiscountRecommendationDTO dto = service.respondToRecommendation(
                500L, 1L, new RespondToDiscountRecommendationRequest(RecommendationDecision.ACCEPT, null));

        assertThat(dto.status()).isEqualTo(RecommendationStatus.ACCEPTED);
        assertThat(dto.suggestedPercentage()).isEqualByComparingTo("40.00");
        assertThat(dto.respondedAt()).isEqualTo(NOW);
        verify(productService).applyDiscount(10L, 1L, Percentage.of(40));
    }

    @Test
    void adjustingARecommendationAppliesTheEstablishmentsOwnPercentage() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        DiscountRecommendationDTO dto = service.respondToRecommendation(500L, 1L,
                new RespondToDiscountRecommendationRequest(
                        RecommendationDecision.ADJUST, BigDecimal.valueOf(25)));

        assertThat(dto.status()).isEqualTo(RecommendationStatus.ADJUSTED);
        assertThat(dto.suggestedPercentage()).isEqualByComparingTo("25.00");
        verify(productService).applyDiscount(10L, 1L, Percentage.of(25));
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
        verify(productService, never()).applyDiscount(any(), any(), any());
    }

    @Test
    void refusingARecommendationLeavesThePriceUnchanged() {
        AiRecommendation recommendation = pending(500L, Percentage.of(40), NOW.minusHours(1));
        when(recommendationRepository.findById(500L)).thenReturn(Optional.of(recommendation));

        DiscountRecommendationDTO dto = service.respondToRecommendation(
                500L, 1L, new RespondToDiscountRecommendationRequest(RecommendationDecision.REFUSE, null));

        assertThat(dto.status()).isEqualTo(RecommendationStatus.REFUSED);
        assertThat(dto.currentPrice()).isEqualByComparingTo("10.00");
        verify(productService, never()).applyDiscount(any(), any(), any());
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
        verify(productService, never()).applyDiscount(any(), any(), any());
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
    void respondingToARecommendationOfAnotherEstablishmentIsRejected() {
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
        assertThat(event.productId()).isEqualTo(10L);
        assertThat(event.establishmentId()).isEqualTo(1L);
        assertThat(event.status()).isEqualTo(RecommendationStatus.ACCEPTED);
        assertThat(event.appliedPercentage()).isEqualByComparingTo("40.00");
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
        when(recommendationRepository.findByProductEstablishmentIdAndStatusAndActiveTrueOrderByCreationDateAsc(
                eq(1L), eq(RecommendationStatus.PENDING))).thenReturn(List.of(fresh, stale));

        List<DiscountRecommendationDTO> pendingList = service.findPendingForEstablishment(1L);

        assertThat(pendingList).extracting(DiscountRecommendationDTO::id).containsExactly(500L);
        assertThat(stale.getStatus()).isEqualTo(RecommendationStatus.EXPIRED);
        verify(recommendationRepository).save(stale);
    }

    @Test
    void findHistoryReturnsEveryRecommendationOfTheProduct() {
        AiRecommendation accepted = pending(500L, Percentage.of(40), NOW.minusDays(2));
        accepted.setStatus(RecommendationStatus.ACCEPTED);
        AiRecommendation refused = pending(501L, Percentage.of(30), NOW.minusDays(3));
        refused.setStatus(RecommendationStatus.REFUSED);
        when(recommendationRepository.findByProductIdAndActiveTrueOrderByCreationDateDesc(10L))
                .thenReturn(List.of(accepted, refused));

        List<DiscountRecommendationDTO> history = service.findHistoryForProduct(10L, 1L);

        assertThat(history).extracting(DiscountRecommendationDTO::id).containsExactly(500L, 501L);
        assertThat(history).extracting(DiscountRecommendationDTO::status)
                .containsExactly(RecommendationStatus.ACCEPTED, RecommendationStatus.REFUSED);
    }

    @Test
    void findHistoryRejectsProductFromAnotherEstablishment() {
        when(productService.findProductOwnedBy(10L, 2L)).thenThrow(new EntityNotFoundException(Product.class, 10L));

        assertThatThrownBy(() -> service.findHistoryForProduct(10L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
