package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.validation.validators.OfferBusinessValidator;
import br.com.seudominio.perishrescue.business.validation.validators.OfferOrderBusinessValidator;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.perishrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.OfferOrder;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.OfferOrderStatus;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.OfferMapper;
import br.com.seudominio.perishrescue.domain.mappers.OfferOrderMapper;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OfferOrderRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OfferRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class OfferOrderServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);
    private static final LocalDateTime CLOSING = LocalDateTime.of(2026, 9, 14, 22, 0);

    private OfferRepository offerRepository;
    private OfferOrderRepository offerOrderRepository;
    private BeneficiaryRepository beneficiaryRepository;
    private OfferService offerService;
    private OfferOrderService offerOrderService;
    private PerishableResource resource;
    private Beneficiary beneficiary;
    private long nextOrderId;

    @BeforeEach
    void setUp() {
        offerRepository = mock(OfferRepository.class);
        offerOrderRepository = mock(OfferOrderRepository.class);
        beneficiaryRepository = mock(BeneficiaryRepository.class);
        ResourceService resourceService = mock(ResourceService.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any())).thenReturn(Collections.emptySet());
        MessageUtils messageUtils = mock(MessageUtils.class);

        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        nextOrderId = 900L;
        when(offerOrderRepository.save(any(OfferOrder.class))).thenAnswer(invocation -> {
            OfferOrder saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(nextOrderId++);
                saved.setCreationDate(NOW);
            }
            return saved;
        });

        offerService = new OfferService(
                offerRepository,
                new OfferMapper(),
                beanValidator,
                messageUtils,
                resourceService,
                new OfferBusinessValidator(beanValidator, timeProvider),
                timeProvider,
                "22:00");

        offerOrderService = new OfferOrderService(
                offerOrderRepository,
                new OfferOrderMapper(),
                beanValidator,
                messageUtils,
                offerService,
                beneficiaryRepository,
                new OfferOrderBusinessValidator(beanValidator));

        Offerer offerer = new Offerer();
        offerer.setId(1L);

        resource = new TestResource();
        resource.setId(10L);
        resource.setName("Pao de queijo");
        resource.setOfferer(offerer);
        resource.setOriginalPrice(Money.of(10.00));
        resource.setCurrentPrice(Money.of(5.20));
        resource.setStockQuantity(20);

        beneficiary = Beneficiary.builder()
                .id(50L)
                .name("Maria")
                .document("52998224725")
                .email("maria@example.com")
                .passwordHash("hashed")
                .build();

        when(beneficiaryRepository.findById(50L)).thenReturn(Optional.of(beneficiary));
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

    @Test
    void reserveDecreasesOfferQuantityAndCreatesReservedOrder() {
        Offer offer = activeOffer(700L, 10);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        OfferOrderDTO dto = offerOrderService.reserve(new ReserveOfferRequest(700L, 3), 50L);

        assertThat(dto.id()).isEqualTo(900L);
        assertThat(dto.offerId()).isEqualTo(700L);
        assertThat(dto.resourceId()).isEqualTo(10L);
        assertThat(dto.resourceName()).isEqualTo("Pao de queijo");
        assertThat(dto.beneficiaryId()).isEqualTo(50L);
        assertThat(dto.quantity()).isEqualTo(3);
        assertThat(dto.unitPrice()).isEqualByComparingTo("5.20");
        assertThat(dto.totalPrice()).isEqualByComparingTo("15.60");
        assertThat(dto.status()).isEqualTo(OfferOrderStatus.RESERVED);
        assertThat(dto.createdAt()).isEqualTo(NOW);

        assertThat(offer.getAvailableQuantity()).isEqualTo(7);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.ACTIVE);
        verify(offerRepository).save(offer);
        verify(offerOrderRepository).save(any(OfferOrder.class));
    }

    @Test
    void reserveClosesOfferWhenLastUnitsAreReserved() {
        Offer offer = activeOffer(700L, 4);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        OfferOrderDTO dto = offerOrderService.reserve(new ReserveOfferRequest(700L, 4), 50L);

        assertThat(dto.status()).isEqualTo(OfferOrderStatus.RESERVED);
        assertThat(offer.getAvailableQuantity()).isZero();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.CLOSED);
    }

    @Test
    void reserveRejectsRequestedQuantityGreaterThanAvailable() {
        Offer offer = activeOffer(700L, 2);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerOrderService.reserve(new ReserveOfferRequest(700L, 5), 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has only 2 units left");

        assertThat(offer.getAvailableQuantity()).isEqualTo(2);
        verify(offerOrderRepository, never()).save(any(OfferOrder.class));
    }

    @Test
    void reserveRejectsExpiredOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setExpiresAt(NOW.minusMinutes(5));
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerOrderService.reserve(new ReserveOfferRequest(700L, 1), 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has expired");

        assertThat(offer.getAvailableQuantity()).isEqualTo(5);
        verify(offerOrderRepository, never()).save(any(OfferOrder.class));
    }

    @Test
    void reserveRejectsClosedOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setStatus(OfferStatus.CLOSED);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerOrderService.reserve(new ReserveOfferRequest(700L, 1), 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is CLOSED");

        verify(offerOrderRepository, never()).save(any(OfferOrder.class));
    }

    @Test
    void reserveRejectsNonPositiveQuantity() {
        Offer offer = activeOffer(700L, 5);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> offerOrderService.reserve(new ReserveOfferRequest(700L, 0), 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("quantity must be greater than zero");

        verify(offerOrderRepository, never()).save(any(OfferOrder.class));
    }

    @Test
    void reserveThrowsEntityNotFoundWhenOfferDoesNotExist() {
        when(offerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerOrderService.reserve(new ReserveOfferRequest(999L, 1), 50L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(offerOrderRepository, never()).save(any(OfferOrder.class));
    }

    @Test
    void reserveThrowsEntityNotFoundWhenBeneficiaryDoesNotExist() {
        when(beneficiaryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offerOrderService.reserve(new ReserveOfferRequest(700L, 1), 999L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(offerOrderRepository, never()).save(any(OfferOrder.class));
    }

    @Test
    void reserveRejectsNullRequest() {
        assertThatThrownBy(() -> offerOrderService.reserve(null, 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("offerId and quantity must be informed");
    }

    @Test
    void findAllForBeneficiaryReturnsBeneficiariesReservations() {
        Offer offer = activeOffer(700L, 5);
        OfferOrder first = OfferOrder.builder()
                .id(901L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(2)
                .build();
        OfferOrder second = OfferOrder.builder()
                .id(900L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(1)
                .build();

        when(offerOrderRepository.findByBeneficiaryIdAndActiveTrueOrderByCreationDateDesc(50L))
                .thenReturn(List.of(first, second));

        List<OfferOrderDTO> result = offerOrderService.findAllForBeneficiary(50L);

        assertThat(result).extracting(OfferOrderDTO::id).containsExactly(901L, 900L);
    }

    @Test
    void findByIdForBeneficiaryReturnsReservationWhenOwnedByBeneficiary() {
        Offer offer = activeOffer(700L, 5);
        OfferOrder order = OfferOrder.builder()
                .id(900L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(2)
                .build();

        when(offerOrderRepository.findById(900L)).thenReturn(Optional.of(order));

        OfferOrderDTO dto = offerOrderService.findByIdForBeneficiary(900L, 50L);

        assertThat(dto.id()).isEqualTo(900L);
        assertThat(dto.beneficiaryId()).isEqualTo(50L);
    }

    @Test
    void findByIdForBeneficiaryThrowsEntityNotFoundWhenOwnedByAnotherBeneficiary() {
        Offer offer = activeOffer(700L, 5);
        OfferOrder order = OfferOrder.builder()
                .id(900L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(2)
                .build();

        when(offerOrderRepository.findById(900L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> offerOrderService.findByIdForBeneficiary(900L, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
