package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.pricing.PriceCalculator;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.business.event.DomainEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.validation.validators.OfferBusinessValidator;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.dtos.CreateOfferRequest;
import br.com.seudominio.perishrescue.domain.dtos.OfferDTO;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.OfferMapper;
import br.com.seudominio.perishrescue.persistence.repositories.OfferRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class OfferServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);
    private static final LocalDateTime CLOSING = LocalDateTime.of(2026, 9, 14, 22, 0);

    private OfferRepository offerRepository;
    private ResourceService resourceService;
    private OfferService service;
    private Offerer offerer;
    private PerishableResource resource;
    private long nextId;

    @BeforeEach
    void setUp() {
        offerRepository = mock(OfferRepository.class);
        resourceService = mock(ResourceService.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        nextId = 800L;
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(nextId++);
                saved.setCreationDate(NOW);
            }
            return saved;
        });

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any())).thenReturn(java.util.Collections.emptySet());

        service = new OfferService(
                offerRepository, new OfferMapper(), beanValidator, mock(MessageUtils.class),
                resourceService, new OfferBusinessValidator(beanValidator, timeProvider),
                timeProvider, new PriceCalculator(context -> Percentage.of(0), 90), mock(DomainEventPublisher.class));

        offerer = new Offerer();
        offerer.setId(1L);

        resource = new TestResource();
        resource.setId(10L);
        resource.setName("Pao de queijo");
        resource.setOfferer(offerer);
        resource.setOriginalPrice(Money.of(10.00));
        resource.setCurrentPrice(Money.of(5.20));
        resource.setStockQuantity(20);

        when(resourceService.findResourceOwnedBy(10L, 1L)).thenReturn(resource);
        when(offerRepository.findFirstByResourceIdAndStatusInAndActiveTrueOrderByCreationDateDesc(
                10L, OfferStatus.openStatuses())).thenReturn(Optional.empty());
    }

    private Offer activeOffer(Long id, int quantity) {
        Offer offer = Offer.builder()
                .id(id)
                .resource(resource)
                .discountedPrice(Money.of(5.20))
                .availableQuantity(quantity)
                .expiresAt(CLOSING)
                .build();
        offer.setCreationDate(NOW);
        return offer;
    }

    // ---------- criacao manual ----------

    @Test
    void createOfferFillsEveryFieldFromTheResourceWhenOnlyTheIdIsGiven() {
        OfferDTO dto = service.createOffer(new CreateOfferRequest(10L, null, null, null), 1L);

        assertThat(dto.id()).isEqualTo(800L);
        assertThat(dto.resourceId()).isEqualTo(10L);
        assertThat(dto.resourceName()).isEqualTo("Pao de queijo");
        assertThat(dto.originalPrice()).isEqualByComparingTo("10.00");
        assertThat(dto.discountedPrice()).isEqualByComparingTo("5.20");
        assertThat(dto.availableQuantity()).isEqualTo(20);
        assertThat(dto.expiresAt()).isEqualTo(resource.criticalDeadline(NOW));
        assertThat(dto.status()).isEqualTo(OfferStatus.AVAILABLE);
        assertThat(dto.priority()).isFalse();
    }

    @Test
    void createOfferHonoursTheInformedFields() {
        OfferDTO dto = service.createOffer(new CreateOfferRequest(
                10L, new BigDecimal("7.00"), 5, NOW.plusHours(2)), 1L);

        assertThat(dto.discountedPrice()).isEqualByComparingTo("7.00");
        assertThat(dto.availableQuantity()).isEqualTo(5);
        assertThat(dto.expiresAt()).isEqualTo(NOW.plusHours(2));
    }

    @Test
    void createOfferRejectsAResourceWithoutStock() {
        resource.setStockQuantity(0);

        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(10L, null, null, null), 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has no stock to offer");
        verify(offerRepository, never()).save(any(Offer.class));
    }

    @Test
    void createOfferRejectsAQuantityAboveTheStock() {
        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(10L, null, 50, null), 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("exceeds the resource stock of 20");
    }

    @Test
    void createOfferRejectsASecondActiveOfferForTheSameResource() {
        when(offerRepository.findFirstByResourceIdAndStatusInAndActiveTrueOrderByCreationDateDesc(
                10L, OfferStatus.openStatuses())).thenReturn(Optional.of(activeOffer(700L, 20)));

        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(10L, null, null, null), 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already has an open offer");
    }

    @Test
    void createOfferRejectsAPriceAboveTheOriginal() {
        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(
                10L, new BigDecimal("15.00"), null, null), 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must not be greater than the resource's original price");
    }

    @Test
    void createOfferRejectsADeadlineInThePast() {
        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(
                10L, null, null, NOW.minusHours(1)), 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expiresAt must be in the future");
    }

    @Test
    void createOfferRejectsAResourceFromAnotherOfferer() {
        when(resourceService.findResourceOwnedBy(10L, 2L)).thenThrow(new EntityNotFoundException(PerishableResource.class, 10L));

        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(10L, null, null, null), 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- criacao automatica (UC07) ----------

    @Test
    void createFromAcceptedDiscountPublishesAnOfferAtTheDiscountedPrice() {
        Optional<OfferDTO> dto = service.createFromAcceptedDiscount(10L, 1L);

        assertThat(dto).isPresent();
        assertThat(dto.get().discountedPrice()).isEqualByComparingTo("5.20");
        assertThat(dto.get().originalPrice()).isEqualByComparingTo("10.00");
        assertThat(dto.get().availableQuantity()).isEqualTo(20);
        assertThat(dto.get().expiresAt()).isEqualTo(resource.criticalDeadline(NOW));
        assertThat(dto.get().status()).isEqualTo(OfferStatus.AVAILABLE);
    }

    @Test
    void createFromAcceptedDiscountSupersedesTheStaleOffer() {
        Offer stale = activeOffer(700L, 20);
        when(offerRepository.findFirstByResourceIdAndStatusInAndActiveTrueOrderByCreationDateDesc(
                10L, OfferStatus.openStatuses())).thenReturn(Optional.of(stale));

        Optional<OfferDTO> dto = service.createFromAcceptedDiscount(10L, 1L);

        assertThat(stale.getStatus()).isEqualTo(OfferStatus.WITHDRAWN);
        assertThat(dto).isPresent();
        assertThat(dto.get().id()).isEqualTo(800L);
        assertThat(dto.get().status()).isEqualTo(OfferStatus.AVAILABLE);
    }

    @Test
    void createFromAcceptedDiscountPublishesNothingWhenThereIsNoStock() {
        resource.setStockQuantity(0);

        assertThat(service.createFromAcceptedDiscount(10L, 1L)).isEmpty();
        verify(offerRepository, never()).save(any(Offer.class));
    }

    // ---------- encerramento ----------

    @Test
    void decreaseQuantityClosesTheOfferWhenNothingIsLeft() {
        Offer offer = activeOffer(700L, 3);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        Offer updated = service.decreaseQuantity(700L, 3);

        assertThat(updated.getAvailableQuantity()).isZero();
        assertThat(updated.getStatus()).isEqualTo(OfferStatus.SOLD_OUT);
    }

    @Test
    void decreaseQuantityKeepsTheOfferActiveWhileUnitsRemain() {
        Offer offer = activeOffer(700L, 10);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        Offer updated = service.decreaseQuantity(700L, 4);

        assertThat(updated.getAvailableQuantity()).isEqualTo(6);
        assertThat(updated.getStatus()).isEqualTo(OfferStatus.AVAILABLE);
    }

    @Test
    void decreaseQuantityRejectsMoreUnitsThanAvailable() {
        Offer offer = activeOffer(700L, 2);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.decreaseQuantity(700L, 5))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has only 2 units left");
        assertThat(offer.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void decreaseQuantityRejectsAClosedOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setStatus(OfferStatus.WITHDRAWN);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.decreaseQuantity(700L, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is WITHDRAWN");
    }

    @Test
    void decreaseQuantityRejectsAnExpiredOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setExpiresAt(NOW.minusMinutes(1));
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.decreaseQuantity(700L, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has expired");
        assertThat(offer.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void decreaseQuantityRejectsANonExistentOffer() {
        when(offerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decreaseQuantity(999L, 1))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void closeOffersForDepletedResourceClosesEveryOpenOffer() {
        Offer first = activeOffer(700L, 5);
        Offer second = activeOffer(701L, 3);
        when(offerRepository.findByResourceIdAndStatusInAndActiveTrue(10L, OfferStatus.openStatuses()))
                .thenReturn(List.of(first, second));

        int closed = service.closeOffersForDepletedResource(10L);

        assertThat(closed).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(OfferStatus.WITHDRAWN);
        assertThat(second.getStatus()).isEqualTo(OfferStatus.WITHDRAWN);
    }

    @Test
    void closeOffersForDepletedResourceIsSafeWhenThereIsNothingOpen() {
        when(offerRepository.findByResourceIdAndStatusInAndActiveTrue(10L, OfferStatus.openStatuses()))
                .thenReturn(List.of());

        assertThat(service.closeOffersForDepletedResource(10L)).isZero();
        verify(offerRepository, never()).save(any(Offer.class));
    }

    @Test
    void closeOfferByHandClosesAnActiveOffer() {
        Offer offer = activeOffer(700L, 5);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThat(service.closeOffer(700L, 1L).status()).isEqualTo(OfferStatus.WITHDRAWN);
    }

    @Test
    void closeOfferRejectsAnAlreadyClosedOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setStatus(OfferStatus.WITHDRAWN);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.closeOffer(700L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is WITHDRAWN");
    }

    @Test
    void closeOfferRejectsAnOfferOfAnotherOfferer() {
        when(offerRepository.findById(700L)).thenReturn(Optional.of(activeOffer(700L, 5)));

        assertThatThrownBy(() -> service.closeOffer(700L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- listagens ----------

    @Test
    void findAllForOffererReturnsTheOfferersOffers() {
        when(offerRepository.findByResourceOffererIdAndActiveTrueOrderByCreationDateDesc(1L))
                .thenReturn(List.of(activeOffer(701L, 5), activeOffer(700L, 3)));

        assertThat(service.findAllForOfferer(1L))
                .extracting(OfferDTO::id).containsExactly(701L, 700L);
    }

    @Test
    void findOrderableAsksTheRepositoryForTheCurrentMoment() {
        when(offerRepository.findOrderable(NOW)).thenReturn(List.of(activeOffer(700L, 5)));

        assertThat(service.findOrderable()).extracting(OfferDTO::id).containsExactly(700L);
        verify(offerRepository).findOrderable(NOW);
    }

    // ---------- UC11 prioridade ----------

    @Test
    void markAsPriorityMarksAnActiveOfferAsPriority() {
        Offer offer = activeOffer(700L, 5);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        OfferDTO result = service.markAsPriority(700L, 1L);

        assertThat(result.priority()).isTrue();
        assertThat(offer.isPriority()).isTrue();
        verify(offerRepository).save(offer);
    }

    @Test
    void markAsPriorityRejectsInactiveOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setStatus(OfferStatus.WITHDRAWN);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.markAsPriority(700L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is WITHDRAWN");
    }

    @Test
    void markAsPriorityRejectsOfferFromAnotherOfferer() {
        Offer offer = activeOffer(700L, 5);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.markAsPriority(700L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void markOrPublishPriorityOfferPrioritizesExistingActiveOffer() {
        Offer existing = activeOffer(700L, 5);
        when(offerRepository.findFirstByResourceIdAndStatusInAndActiveTrueOrderByCreationDateDesc(10L, OfferStatus.openStatuses()))
                .thenReturn(Optional.of(existing));

        Optional<OfferDTO> result = service.markOrPublishPriorityOfferForResource(10L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().priority()).isTrue();
        assertThat(existing.isPriority()).isTrue();
        verify(offerRepository).save(existing);
    }

    @Test
    void markOrPublishPriorityOfferPublishesNewPriorityOfferWhenNoneExists() {
        when(offerRepository.findFirstByResourceIdAndStatusInAndActiveTrueOrderByCreationDateDesc(10L, OfferStatus.openStatuses()))
                .thenReturn(Optional.empty());

        Optional<OfferDTO> result = service.markOrPublishPriorityOfferForResource(10L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().priority()).isTrue();
    }

    @Test
    void markOrPublishPriorityOfferReturnsEmptyWhenStockIsZero() {
        resource.setStockQuantity(0);

        Optional<OfferDTO> result = service.markOrPublishPriorityOfferForResource(10L, 1L);

        assertThat(result).isEmpty();
        verify(offerRepository, never()).save(any(Offer.class));
    }

    // ---------- E4/E5: expiration and repricing ----------

    @Test
    void expireOverdueOffersExpiresEveryOpenOfferPastItsDeadline() {
        Offer available = activeOffer(700L, 3);
        Offer soldOut = activeOffer(701L, 1);
        soldOut.takeUnits(1);
        when(offerRepository.findByStatusInAndExpiresAtLessThanEqualAndActiveTrue(OfferStatus.openStatuses(), NOW))
                .thenReturn(List.of(available, soldOut));

        assertThat(service.expireOverdueOffers()).isEqualTo(2);
        assertThat(available.getStatus()).isEqualTo(OfferStatus.EXPIRED);
        assertThat(soldOut.getStatus()).isEqualTo(OfferStatus.EXPIRED);
    }

    @Test
    void repriceLowersAnOfferWhenThePolicyDiscountGrewButNeverRaisesIt() {
        TimeProvider clock = mock(TimeProvider.class);
        when(clock.now()).thenReturn(NOW);
        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any())).thenReturn(java.util.Collections.emptySet());
        OfferService seventyOff = new OfferService(
                offerRepository, new OfferMapper(), beanValidator, mock(MessageUtils.class), resourceService,
                new OfferBusinessValidator(beanValidator, clock), clock,
                new PriceCalculator(context -> Percentage.of(70), 90), mock(DomainEventPublisher.class));
        Offer expensive = activeOffer(700L, 3);   // 5.20, original 10.00 -> 70% off = 3.00
        Offer cheap = activeOffer(701L, 3);
        cheap.setDiscountedPrice(Money.of(2.50)); // already below 3.00: stays
        when(offerRepository.findByStatusInAndExpiresAtGreaterThanAndActiveTrue(OfferStatus.openStatuses(), NOW))
                .thenReturn(List.of(expensive, cheap));

        assertThat(seventyOff.repriceOpenOffers()).isEqualTo(1);
        assertThat(expensive.getDiscountedPrice().amount()).isEqualByComparingTo("3.00");
        assertThat(cheap.getDiscountedPrice().amount()).isEqualByComparingTo("2.50");
    }

    @Test
    void returnUnitsReopensASoldOutOfferStillWithinItsDeadline() {
        Offer offer = activeOffer(700L, 2);
        offer.takeUnits(2);

        assertThat(service.returnUnits(offer, 2)).isTrue();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.AVAILABLE);

        offer.setExpiresAt(NOW.minusMinutes(1));
        assertThat(service.returnUnits(offer, 1)).isFalse();
    }
}
