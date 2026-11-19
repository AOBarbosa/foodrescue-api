package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.pricing.PriceCalculator;
import br.com.seudominio.perishrescue.business.pricing.PricingPolicy;
import br.com.seudominio.perishrescue.business.rescue.RescueRule;
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
import br.com.seudominio.perishrescue.business.validation.validators.ReservationBusinessValidator;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.domain.dtos.ReservationDTO;
import br.com.seudominio.perishrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.Reservation;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.OfferMapper;
import br.com.seudominio.perishrescue.domain.mappers.ReservationMapper;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ReservationRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OfferRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class ReservationServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);
    private static final LocalDateTime CLOSING = LocalDateTime.of(2026, 9, 14, 22, 0);

    private OfferRepository offerRepository;
    private ReservationRepository reservationRepository;
    private BeneficiaryRepository beneficiaryRepository;
    private OfferService offerService;
    private ReservationService reservationService;
    private PerishableResource resource;
    private Beneficiary beneficiary;
    private long nextOrderId;
    private Validator beanValidator;
    private MessageUtils messageUtils;
    private ResourceService resourceService;
    private TimeProvider timeProvider;
    private DomainEventPublisher eventPublisher;

    private ReservationService serviceWith(List<RescueRule> rules, PricingPolicy pricingPolicy) {
        PriceCalculator priceCalculator = new PriceCalculator(pricingPolicy, 90);
        offerService = new OfferService(
                offerRepository, new OfferMapper(), beanValidator, messageUtils, resourceService,
                new OfferBusinessValidator(beanValidator, timeProvider), timeProvider, priceCalculator, eventPublisher);
        return new ReservationService(
                reservationRepository, new ReservationMapper(), beanValidator, messageUtils, offerService,
                offerRepository, resourceService, beneficiaryRepository,
                new ReservationBusinessValidator(beanValidator), rules,
                (beneficiaryId, from, to) -> 0, priceCalculator, timeProvider, eventPublisher, 120);
    }

    @BeforeEach
    void setUp() {
        offerRepository = mock(OfferRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        beneficiaryRepository = mock(BeneficiaryRepository.class);
        ResourceService resourceService = mock(ResourceService.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any())).thenReturn(Collections.emptySet());
        MessageUtils messageUtils = mock(MessageUtils.class);

        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        nextOrderId = 900L;
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(nextOrderId++);
                saved.setCreationDate(NOW);
            }
            return saved;
        });

        this.beanValidator = beanValidator;
        this.messageUtils = messageUtils;
        this.resourceService = resourceService;
        this.timeProvider = timeProvider;
        eventPublisher = mock(DomainEventPublisher.class);
        reservationService = serviceWith(List.of(), context -> Percentage.of(0));

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

        ReservationDTO dto = reservationService.reserve(new ReserveOfferRequest(700L, 3), 50L);

        assertThat(dto.id()).isEqualTo(900L);
        assertThat(dto.offerId()).isEqualTo(700L);
        assertThat(dto.resourceId()).isEqualTo(10L);
        assertThat(dto.resourceName()).isEqualTo("Pao de queijo");
        assertThat(dto.beneficiaryId()).isEqualTo(50L);
        assertThat(dto.quantity()).isEqualTo(3);
        assertThat(dto.unitPrice()).isEqualByComparingTo("5.20");
        assertThat(dto.totalPrice()).isEqualByComparingTo("15.60");
        assertThat(dto.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(dto.createdAt()).isEqualTo(NOW);

        assertThat(offer.getAvailableQuantity()).isEqualTo(7);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.AVAILABLE);
        verify(offerRepository).save(offer);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void reserveClosesOfferWhenLastUnitsAreReserved() {
        Offer offer = activeOffer(700L, 4);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        ReservationDTO dto = reservationService.reserve(new ReserveOfferRequest(700L, 4), 50L);

        assertThat(dto.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(offer.getAvailableQuantity()).isZero();
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.SOLD_OUT);
    }

    @Test
    void reserveRejectsRequestedQuantityGreaterThanAvailable() {
        Offer offer = activeOffer(700L, 2);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> reservationService.reserve(new ReserveOfferRequest(700L, 5), 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has only 2 units left");

        assertThat(offer.getAvailableQuantity()).isEqualTo(2);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserveRejectsExpiredOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setExpiresAt(NOW.minusMinutes(5));
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> reservationService.reserve(new ReserveOfferRequest(700L, 1), 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has expired");

        assertThat(offer.getAvailableQuantity()).isEqualTo(5);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserveRejectsClosedOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setStatus(OfferStatus.WITHDRAWN);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> reservationService.reserve(new ReserveOfferRequest(700L, 1), 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is WITHDRAWN");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserveRejectsNonPositiveQuantity() {
        Offer offer = activeOffer(700L, 5);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> reservationService.reserve(new ReserveOfferRequest(700L, 0), 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("quantity must be greater than zero");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserveThrowsEntityNotFoundWhenOfferDoesNotExist() {
        when(offerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserve(new ReserveOfferRequest(999L, 1), 50L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserveThrowsEntityNotFoundWhenBeneficiaryDoesNotExist() {
        when(beneficiaryRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.reserve(new ReserveOfferRequest(700L, 1), 999L))
                .isInstanceOf(EntityNotFoundException.class);

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void reserveRejectsNullRequest() {
        assertThatThrownBy(() -> reservationService.reserve(null, 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("offerId and quantity must be informed");
    }

    @Test
    void findAllForBeneficiaryReturnsBeneficiariesReservations() {
        Offer offer = activeOffer(700L, 5);
        Reservation first = Reservation.builder()
                .id(901L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(2)
                .build();
        Reservation second = Reservation.builder()
                .id(900L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(1)
                .build();

        when(reservationRepository.findByBeneficiaryIdAndActiveTrueOrderByCreationDateDesc(50L))
                .thenReturn(List.of(first, second));

        List<ReservationDTO> result = reservationService.findAllForBeneficiary(50L);

        assertThat(result).extracting(ReservationDTO::id).containsExactly(901L, 900L);
    }

    @Test
    void findByIdForBeneficiaryReturnsReservationWhenOwnedByBeneficiary() {
        Offer offer = activeOffer(700L, 5);
        Reservation order = Reservation.builder()
                .id(900L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(2)
                .build();

        when(reservationRepository.findById(900L)).thenReturn(Optional.of(order));

        ReservationDTO dto = reservationService.findByIdForBeneficiary(900L, 50L);

        assertThat(dto.id()).isEqualTo(900L);
        assertThat(dto.beneficiaryId()).isEqualTo(50L);
    }

    @Test
    void findByIdForBeneficiaryThrowsEntityNotFoundWhenOwnedByAnotherBeneficiary() {
        Offer offer = activeOffer(700L, 5);
        Reservation order = Reservation.builder()
                .id(900L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(2)
                .build();

        when(reservationRepository.findById(900L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> reservationService.findByIdForBeneficiary(900L, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- E4: lifecycle (pickup, cancel, expiration) ----------

    private Reservation heldReservation(Offer offer, int quantity) {
        Reservation reservation = Reservation.builder()
                .id(950L)
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(quantity)
                .unitPrice(Money.of(5.20))
                .pickupDeadline(NOW.plusHours(2))
                .pickupCode("ABC234")
                .build();
        when(reservationRepository.findById(950L)).thenReturn(Optional.of(reservation));
        return reservation;
    }

    @Test
    void reserveFreezesThePriceAndGivesAPickupCodeAndDeadline() {
        Offer offer = activeOffer(700L, 10);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        ReservationDTO dto = reservationService.reserve(new ReserveOfferRequest(700L, 1), 50L);

        assertThat(dto.pickupCode()).hasSize(6).matches("[A-Z2-9]+");
        assertThat(dto.pickupDeadline()).isEqualTo(NOW.plusMinutes(120));
        assertThat(dto.unitPrice()).isEqualByComparingTo("5.20");
    }

    @Test
    void thePickupDeadlineNeverGoesPastTheOffersOwnDeadline() {
        Offer offer = activeOffer(700L, 10);
        offer.setExpiresAt(NOW.plusMinutes(30));
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        ReservationDTO dto = reservationService.reserve(new ReserveOfferRequest(700L, 1), 50L);

        assertThat(dto.pickupDeadline()).isEqualTo(NOW.plusMinutes(30));
    }

    @Test
    void reserveUsesTheBeneficiarysPriceFromThePricingPolicy() {
        reservationService = serviceWith(List.of(), context -> Percentage.of(70));
        Offer offer = activeOffer(700L, 10);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        ReservationDTO dto = reservationService.reserve(new ReserveOfferRequest(700L, 2), 50L);

        assertThat(dto.unitPrice()).isEqualByComparingTo("3.00");
        assertThat(dto.totalPrice()).isEqualByComparingTo("6.00");
    }

    @Test
    void reserveCollectsEveryRescueRuleViolationAndTakesNoUnits() {
        RescueRule tooMany = context -> Optional.of(new br.com.seudominio.perishrescue.core.validation.ValidationError(
                "daily limit exceeded", "quantity", context.quantity(), "RESCUE_QUOTA_EXCEEDED"));
        RescueRule closed = context -> Optional.of(new br.com.seudominio.perishrescue.core.validation.ValidationError(
                "outside business hours", "offerId", null, "OUTSIDE_BUSINESS_HOURS"));
        RescueRule fine = context -> Optional.empty();
        reservationService = serviceWith(List.of(tooMany, fine, closed), context -> Percentage.of(0));
        Offer offer = activeOffer(700L, 10);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> reservationService.reserve(new ReserveOfferRequest(700L, 5), 50L))
                .isInstanceOf(br.com.seudominio.perishrescue.core.validation.exception.ValidationException.class)
                .satisfies(ex -> assertThat(((br.com.seudominio.perishrescue.core.validation.exception.ValidationException) ex)
                        .getErrors())
                        .extracting(br.com.seudominio.perishrescue.core.validation.ValidationError::code)
                        .containsExactly("RESCUE_QUOTA_EXCEEDED", "OUTSIDE_BUSINESS_HOURS"));

        assertThat(offer.getAvailableQuantity()).isEqualTo(10);
        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void confirmPickupWithTheRightCodeRescuesAndReleasesTheStock() {
        Offer offer = activeOffer(700L, 8);
        Reservation reservation = heldReservation(offer, 2);
        resource.getOfferer().setId(1L);

        ReservationDTO dto = reservationService.confirmPickup(950L, 1L, " abc234 ");

        assertThat(dto.status()).isEqualTo(ReservationStatus.RESCUED);
        assertThat(dto.pickupCode()).isNull();
        assertThat(reservation.getRescuedAt()).isEqualTo(NOW);
        verify(resourceService).releaseRescuedUnits(resource, 2);
    }

    @Test
    void confirmPickupRejectsAWrongCodeALatePickupAndAnotherOfferer() {
        Offer offer = activeOffer(700L, 8);
        Reservation reservation = heldReservation(offer, 2);

        assertThatThrownBy(() -> reservationService.confirmPickup(950L, 1L, "WRONG1"))
                .hasMessageContaining("pickup code does not match");
        assertThatThrownBy(() -> reservationService.confirmPickup(950L, 2L, "ABC234"))
                .isInstanceOf(EntityNotFoundException.class);

        reservation.setPickupDeadline(NOW.minusMinutes(1));
        assertThatThrownBy(() -> reservationService.confirmPickup(950L, 1L, "ABC234"))
                .hasMessageContaining("pickup window ended");
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RESERVED);
    }

    @Test
    void cancelGivesTheUnitsBackAndReopensASoldOutOffer() {
        Offer offer = activeOffer(700L, 2);
        offer.takeUnits(2);
        heldReservation(offer, 2);

        ReservationDTO dto = reservationService.cancel(950L, 50L);

        assertThat(dto.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(offer.getAvailableQuantity()).isEqualTo(2);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.AVAILABLE);
    }

    @Test
    void cancelRefusesAReservationAlreadyRescued() {
        Reservation reservation = heldReservation(activeOffer(700L, 2), 1);
        reservation.setStatus(ReservationStatus.RESCUED);

        assertThatThrownBy(() -> reservationService.cancel(950L, 50L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("cannot move from RESCUED to CANCELLED");
    }

    @Test
    void expireOverdueReservationsExpiresThemAndGivesTheirUnitsBack() {
        Offer offer = activeOffer(700L, 5);
        Reservation reservation = heldReservation(offer, 3);
        when(reservationRepository.findByStatusAndPickupDeadlineLessThanEqualAndActiveTrue(
                ReservationStatus.RESERVED, NOW)).thenReturn(List.of(reservation));

        int expired = reservationService.expireOverdueReservations();

        assertThat(expired).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(offer.getAvailableQuantity()).isEqualTo(8);
    }
}
