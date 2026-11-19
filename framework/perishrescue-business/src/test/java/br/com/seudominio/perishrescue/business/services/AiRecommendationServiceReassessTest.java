package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.event.DomainEventPublisher;
import br.com.seudominio.perishrescue.business.strategy.DiscountThenPriorityStrategy;
import br.com.seudominio.perishrescue.business.strategy.RiskWeightedDiscountStrategy;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.perishrescue.domain.dtos.WasteRiskDTO;
import br.com.seudominio.perishrescue.domain.entities.AiRecommendation;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;
import br.com.seudominio.perishrescue.domain.mappers.AiRecommendationMapper;
import br.com.seudominio.perishrescue.persistence.repositories.AiRecommendationRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class AiRecommendationServiceReassessTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 20, 0);

    private AiRecommendationRepository recommendationRepository;
    private WasteRiskService wasteRiskService;
    private TimeProvider timeProvider;
    private AiRecommendationService service;
    private PerishableResource resource;
    private AiRecommendation tail;

    @BeforeEach
    void setUp() {
        recommendationRepository = mock(AiRecommendationRepository.class);
        wasteRiskService = mock(WasteRiskService.class);
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);
        when(recommendationRepository.save(any(AiRecommendation.class))).thenAnswer(invocation -> {
            AiRecommendation saved = invocation.getArgument(0);
            saved.setId(501L);
            saved.setCreationDate(NOW);
            return saved;
        });

        service = new AiRecommendationService(
                recommendationRepository, new AiRecommendationMapper(), mock(Validator.class),
                mock(MessageUtils.class), mock(ResourceService.class), wasteRiskService,
                new RiskWeightedDiscountStrategy(0.6, 15, 4, 60), mock(DomainEventPublisher.class),
                timeProvider, "22:00", 24, new DiscountThenPriorityStrategy(10, 60), 3, 60);

        Offerer offerer = new Offerer();
        offerer.setId(1L);
        resource = new TestResource();
        resource.setId(10L);
        resource.setName("Pao frances");
        resource.setOfferer(offerer);
        resource.setStockQuantity(20);
        resource.setOriginalPrice(Money.of(10.00));
        resource.setCurrentPrice(Money.of(6.00));

        tail = AiRecommendation.builder()
                .id(500L)
                .resource(resource)
                .type(RecommendationType.DISCOUNT)
                .suggestedPercentage(Percentage.of(40))
                .status(RecommendationStatus.ACCEPTED)
                .build();
        tail.setCreationDate(NOW.minusHours(3));

        when(recommendationRepository.findChainTailsSince(NOW.toLocalDate().atStartOfDay()))
                .thenReturn(List.of(tail));
        when(recommendationRepository.findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING)).thenReturn(Optional.empty());
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(true));
    }

    private WasteRiskDTO risk(boolean atRisk) {
        return new WasteRiskDTO(10L, "Pao frances", 20, 4, 16,
                BigDecimal.valueOf(80), BigDecimal.valueOf(70), atRisk, 99L, NOW);
    }

    @Test
    void generatesChainedAdditionalDiscountWhenRiskPersistsNearClosing() {
        List<DiscountRecommendationDTO> generated = service.reassessSurplusDestination();

        assertThat(generated).hasSize(1);
        DiscountRecommendationDTO dto = generated.get(0);
        assertThat(dto.previousRecommendationId()).isEqualTo(500L);
        assertThat(dto.type()).isEqualTo(RecommendationType.DISCOUNT);
        assertThat(dto.status()).isEqualTo(RecommendationStatus.PENDING);
        assertThat(dto.suggestedPercentage()).isEqualByComparingTo("50.00");
    }

    @Test
    void prioritizesWhenTheMaximumDiscountWasAlreadyReached() {
        resource.setCurrentPrice(Money.of(4.00));

        List<DiscountRecommendationDTO> generated = service.reassessSurplusDestination();

        assertThat(generated).hasSize(1);
        assertThat(generated.get(0).type()).isEqualTo(RecommendationType.SURPLUS_DESTINATION);
        assertThat(generated.get(0).suggestedPercentage()).isNull();
        assertThat(generated.get(0).previousRecommendationId()).isEqualTo(500L);
    }

    @Test
    void generatesNothingWhenTheResourceIsFullySold() {
        resource.setStockQuantity(0);

        assertThat(service.reassessSurplusDestination()).isEmpty();
        verify(recommendationRepository, never()).save(any(AiRecommendation.class));
    }

    @Test
    void generatesNothingWhenRiskIsNoLongerHigh() {
        when(wasteRiskService.assess(10L, 1L)).thenReturn(risk(false));

        assertThat(service.reassessSurplusDestination()).isEmpty();
    }

    @Test
    void generatesNothingWhenThereIsStillTimeToSell() {
        when(timeProvider.now()).thenReturn(NOW.withHour(15));

        assertThat(service.reassessSurplusDestination()).isEmpty();
        verify(recommendationRepository, never()).findChainTailsSince(any());
    }

    @Test
    void generatesNothingWhenARecommendationIsAlreadyPending() {
        when(recommendationRepository.findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, RecommendationStatus.PENDING)).thenReturn(Optional.of(tail));

        assertThat(service.reassessSurplusDestination()).isEmpty();
    }

    @Test
    void generatesNothingDuringTheCooldown() {
        tail.setCreationDate(NOW.minusMinutes(10));

        assertThat(service.reassessSurplusDestination()).isEmpty();
    }

    @Test
    void chainIsReturnedOldestFirst() {
        AiRecommendation next = AiRecommendation.builder().id(501L).resource(resource)
                .type(RecommendationType.DISCOUNT).previousRecommendationId(500L).build();
        when(recommendationRepository
                .findByResourceIdAndCreationDateGreaterThanEqualAndActiveTrueOrderByCreationDateAsc(
                        10L, NOW.toLocalDate().atStartOfDay()))
                .thenReturn(List.of(tail, next));
        ResourceService resourceService = mock(ResourceService.class);
        when(resourceService.findResourceOwnedBy(10L, 1L)).thenReturn(resource);
        AiRecommendationService local = new AiRecommendationService(
                recommendationRepository, new AiRecommendationMapper(), mock(Validator.class),
                mock(MessageUtils.class), resourceService, wasteRiskService,
                new RiskWeightedDiscountStrategy(0.6, 15, 4, 60), mock(DomainEventPublisher.class),
                timeProvider, "22:00", 24, new DiscountThenPriorityStrategy(10, 60), 3, 60);

        List<DiscountRecommendationDTO> chain = local.findChainForResource(10L, 1L);

        assertThat(chain).extracting(DiscountRecommendationDTO::id).containsExactly(500L, 501L);
        assertThat(chain.get(1).previousRecommendationId()).isEqualTo(500L);
    }
}
