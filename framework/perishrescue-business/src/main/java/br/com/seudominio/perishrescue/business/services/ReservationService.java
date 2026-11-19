package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.event.DomainEventPublisher;
import br.com.seudominio.perishrescue.business.event.ReservationStatusChangedEvent;
import br.com.seudominio.perishrescue.business.pricing.PriceCalculator;
import br.com.seudominio.perishrescue.business.rescue.RescueContext;
import br.com.seudominio.perishrescue.business.rescue.RescueRule;
import br.com.seudominio.perishrescue.business.rescue.ReservationHistory;
import br.com.seudominio.perishrescue.business.validation.validators.ReservationBusinessValidator;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.ValidationError;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.dtos.ReservationDTO;
import br.com.seudominio.perishrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.Reservation;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.ReservationMapper;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OfferRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ReservationRepository;

import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The kernel's reservation flow (UC09, <b>frozen spot</b>): beneficiaries hold
 * units of an offer, then pick them up at the offerer or lose them.
 *
 * <p>{@link #reserve} is the Template Method of the framework: the steps and
 * their order are fixed, and two of them call the instance's hot spots.</p>
 * <ol>
 *   <li>load the beneficiary and the offer;</li>
 *   <li>check every {@link RescueRule} the instance declared (hot spot 3),
 *       collecting all violations into one {@link ValidationException};</li>
 *   <li>price one unit for this beneficiary through the {@link PriceCalculator}
 *       (hot spot 2) and freeze it on the reservation;</li>
 *   <li>take the units from the offer, atomically ({@code @Version});</li>
 *   <li>create the reservation with a pickup code and a pickup deadline
 *       (now + {@code perishrescue.reservation.pickup-tolerance-minutes},
 *       but never after the offer's own deadline);</li>
 *   <li>publish a {@link ReservationStatusChangedEvent}.</li>
 * </ol>
 *
 * <p>Afterwards a reservation is {@linkplain #confirmPickup rescued} by the
 * offerer with the pickup code (the units leave the resource's stock),
 * {@linkplain #cancel cancelled} by the beneficiary, or
 * {@linkplain #expireOverdueReservations expired} by the kernel when the
 * pickup window passes; the last two give the units back to the offer.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
@Transactional
public class ReservationService extends GenericService<Reservation, ReservationDTO> {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final OfferService offerService;
    private final OfferRepository offerRepository;
    private final ResourceService resourceService;
    private final BeneficiaryRepository beneficiaryRepository;
    private final ReservationBusinessValidator reservationValidator;
    private final List<RescueRule> rescueRules;
    private final ReservationHistory reservationHistory;
    private final PriceCalculator priceCalculator;
    private final TimeProvider timeProvider;
    private final DomainEventPublisher eventPublisher;
    private final Duration pickupTolerance;
    private final SecureRandom random = new SecureRandom();

    /**
     * Constructor.
     *
     * @param repository             the reservation repository
     * @param mapper                 the reservation mapper
     * @param validator              the bean validator
     * @param messageUtils           the message utils
     * @param offerService           the offer service, which owns the offer's units and status
     * @param offerRepository        the offer repository
     * @param resourceService        the resource service, which owns the stock
     * @param beneficiaryRepository  the beneficiary repository
     * @param reservationValidator   the business validator for reservations
     * @param rescueRules            the instance's rescue rules (hot spot 3); may be empty
     * @param reservationHistory     read-only history for the rescue rules
     * @param priceCalculator        the kernel's pricing rules (hot spot 2)
     * @param timeProvider           the current date and time provider
     * @param eventPublisher         the domain event publisher
     * @param pickupToleranceMinutes how long a reservation waits for pickup
     */
    public ReservationService(
            ReservationRepository repository,
            ReservationMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            OfferService offerService,
            OfferRepository offerRepository,
            ResourceService resourceService,
            BeneficiaryRepository beneficiaryRepository,
            ReservationBusinessValidator reservationValidator,
            List<RescueRule> rescueRules,
            ReservationHistory reservationHistory,
            PriceCalculator priceCalculator,
            TimeProvider timeProvider,
            DomainEventPublisher eventPublisher,
            @Value("${perishrescue.reservation.pickup-tolerance-minutes:120}") long pickupToleranceMinutes) {
        super(repository, mapper, validator, messageUtils);
        this.reservationRepository = repository;
        this.reservationMapper = mapper;
        this.offerService = offerService;
        this.offerRepository = offerRepository;
        this.resourceService = resourceService;
        this.beneficiaryRepository = beneficiaryRepository;
        this.reservationValidator = reservationValidator;
        this.rescueRules = rescueRules == null ? List.of() : List.copyOf(rescueRules);
        this.reservationHistory = reservationHistory;
        this.priceCalculator = priceCalculator;
        this.timeProvider = timeProvider;
        this.eventPublisher = eventPublisher;
        this.pickupTolerance = Duration.ofMinutes(pickupToleranceMinutes);
    }

    /**
     * Reserves units of an offer for the authenticated beneficiary.
     *
     * @param request       the offer id, the quantity and optional extra attributes for the rules
     * @param beneficiaryId the authenticated beneficiary identifier
     * @return the created reservation
     * @throws EntityNotFoundException        if the offer or the beneficiary does not exist
     * @throws ValidationException            if a rescue rule refuses the reservation
     * @throws BusinessRuleViolationException if the request is incomplete, the offer is not available,
     *                                        has expired, or does not have enough units
     */
    public ReservationDTO reserve(ReserveOfferRequest request, Long beneficiaryId) {
        if (request == null || request.offerId() == null || request.quantity() == null) {
            throw new BusinessRuleViolationException("offerId and quantity must be informed");
        }
        return reserve(request.offerId(), beneficiaryId, request.quantity(), request.attributes());
    }

    /**
     * Reserves units of an offer for the authenticated beneficiary, without
     * extra attributes.
     *
     * @param offerId       the offer identifier
     * @param beneficiaryId the authenticated beneficiary identifier
     * @param quantity      the desired quantity
     * @return the created reservation
     * @see #reserve(Long, Long, int, Map)
     */
    public ReservationDTO reserve(Long offerId, Long beneficiaryId, int quantity) {
        return reserve(offerId, beneficiaryId, quantity, Map.of());
    }

    /**
     * The reservation Template Method: the fixed sequence described in the
     * class documentation.
     *
     * @param offerId       the offer identifier
     * @param beneficiaryId the authenticated beneficiary identifier
     * @param quantity      the desired quantity
     * @param attributes    extra request data for instance-specific rules
     * @return the created reservation
     */
    public ReservationDTO reserve(Long offerId, Long beneficiaryId, int quantity, Map<String, Object> attributes) {
        if (offerId == null) {
            throw new BusinessRuleViolationException("offerId must be informed");
        }
        if (quantity <= 0) {
            throw new BusinessRuleViolationException("quantity must be greater than zero");
        }
        LocalDateTime now = timeProvider.now();

        // 1. load the participants
        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new EntityNotFoundException(Beneficiary.class, beneficiaryId));
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException(Offer.class, offerId));

        // 2. hot spot 3: the instance's rescue rules
        checkRescueRules(new RescueContext(offer, beneficiary, quantity, now, attributes, reservationHistory));

        // 3. hot spot 2: unit price for this beneficiary, frozen on the reservation
        var unitPrice = priceCalculator.unitPriceFor(offer, beneficiary, now);

        // 4. take the units (frozen invariants: available, not expired, enough units)
        Offer reserved = offerService.decreaseQuantity(offerId, quantity);

        // 5. create the reservation
        LocalDateTime pickupDeadline = earliest(now.plus(pickupTolerance), reserved.getExpiresAt());
        Reservation reservation = Reservation.builder()
                .offer(reserved)
                .beneficiary(beneficiary)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .pickupDeadline(pickupDeadline)
                .pickupCode(newPickupCode())
                .status(ReservationStatus.RESERVED)
                .build();
        reservationValidator.validateOperation(reservation, BusinessOperation.CREATE);
        Reservation saved = reservationRepository.save(reservation);

        // 6. announce it
        eventPublisher.publish(new ReservationStatusChangedEvent(
                saved.getId(), offerId, beneficiaryId, null, ReservationStatus.RESERVED));
        return reservationMapper.toDto(saved);
    }

    /**
     * Confirms the pickup of a reservation at the offerer: the reservation
     * becomes {@link ReservationStatus#RESCUED} and its units leave the
     * resource's stock.
     *
     * @param reservationId the reservation identifier
     * @param offererId     the authenticated offerer identifier
     * @param pickupCode    the code the beneficiary showed
     * @return the rescued reservation
     * @throws EntityNotFoundException        if the reservation does not exist or is for another offerer
     * @throws BusinessRuleViolationException if the code does not match, the pickup window has passed
     *                                        or the reservation is no longer held
     */
    public ReservationDTO confirmPickup(Long reservationId, Long offererId, String pickupCode) {
        Reservation reservation = findForOfferer(reservationId, offererId);

        if (reservation.getStatus() != ReservationStatus.RESERVED) {
            throw new BusinessRuleViolationException(String.format(
                    "reservation %d is not waiting for pickup: current status is %s",
                    reservationId, reservation.getStatus()));
        }
        if (pickupCode == null || !reservation.getPickupCode().equalsIgnoreCase(pickupCode.trim())) {
            throw new BusinessRuleViolationException("pickup code does not match");
        }
        LocalDateTime now = timeProvider.now();
        if (now.isAfter(reservation.getPickupDeadline())) {
            throw new BusinessRuleViolationException(String.format(
                    "reservation %d pickup window ended at %s", reservationId, reservation.getPickupDeadline()));
        }

        reservation.transitionTo(ReservationStatus.RESCUED);
        reservation.setRescuedAt(now);
        resourceService.releaseRescuedUnits(reservation.getOffer().getResource(), reservation.getQuantity());
        return reservationMapper.toDto(announce(reservationRepository.save(reservation), ReservationStatus.RESERVED))
                .withoutPickupCode();
    }

    /**
     * Cancels one of the authenticated beneficiary's reservations and gives
     * its units back to the offer.
     *
     * @param reservationId the reservation identifier
     * @param beneficiaryId the authenticated beneficiary identifier
     * @return the cancelled reservation
     * @throws EntityNotFoundException        if the reservation does not exist or belongs to another beneficiary
     * @throws BusinessRuleViolationException if the reservation is no longer held
     */
    public ReservationDTO cancel(Long reservationId, Long beneficiaryId) {
        Reservation reservation = findForBeneficiary(reservationId, beneficiaryId);

        reservation.transitionTo(ReservationStatus.CANCELLED);
        offerService.returnUnits(reservation.getOffer(), reservation.getQuantity());
        return reservationMapper.toDto(announce(reservationRepository.save(reservation), ReservationStatus.RESERVED));
    }

    /**
     * Expires every reservation whose pickup window has passed and gives its
     * units back to the offer. Run periodically by the kernel's
     * {@code ExpirationMonitor}.
     *
     * @return how many reservations expired
     */
    public int expireOverdueReservations() {
        List<Reservation> overdue = reservationRepository
                .findByStatusAndPickupDeadlineLessThanEqualAndActiveTrue(ReservationStatus.RESERVED, timeProvider.now());

        for (Reservation reservation : overdue) {
            reservation.transitionTo(ReservationStatus.EXPIRED);
            offerService.returnUnits(reservation.getOffer(), reservation.getQuantity());
            announce(reservationRepository.save(reservation), ReservationStatus.RESERVED);
        }
        return overdue.size();
    }

    /**
     * Lists every active reservation made by the authenticated beneficiary, newest first.
     *
     * @param beneficiaryId the authenticated beneficiary identifier
     * @return the beneficiary's reservations
     */
    @Transactional(readOnly = true)
    public List<ReservationDTO> findAllForBeneficiary(Long beneficiaryId) {
        return reservationRepository.findByBeneficiaryIdAndActiveTrueOrderByCreationDateDesc(beneficiaryId)
                .stream()
                .map(reservationMapper::toDto)
                .toList();
    }

    /**
     * Lists every active reservation made on the authenticated offerer's offers, newest first.
     *
     * @param offererId the authenticated offerer identifier
     * @return the reservations on the offerer's offers
     */
    @Transactional(readOnly = true)
    public List<ReservationDTO> findAllForOfferer(Long offererId) {
        return reservationRepository.findByOfferResourceOffererIdAndActiveTrueOrderByCreationDateDesc(offererId)
                .stream()
                .map(reservationMapper::toDto)
                .map(ReservationDTO::withoutPickupCode)
                .toList();
    }

    /**
     * Fetches one reservation of the authenticated beneficiary.
     *
     * @param id            the reservation identifier
     * @param beneficiaryId the authenticated beneficiary identifier
     * @return the reservation
     * @throws EntityNotFoundException if the reservation does not exist or belongs to another beneficiary
     */
    @Transactional(readOnly = true)
    public ReservationDTO findByIdForBeneficiary(Long id, Long beneficiaryId) {
        return reservationMapper.toDto(findForBeneficiary(id, beneficiaryId));
    }

    private void checkRescueRules(RescueContext context) {
        List<ValidationError> violations = rescueRules.stream()
                .map(rule -> rule.check(context))
                .flatMap(java.util.Optional::stream)
                .toList();
        if (!violations.isEmpty()) {
            throw new ValidationException("Reservation refused: " + violations.stream()
                    .map(ValidationError::message)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse(""), violations);
        }
    }

    private Reservation announce(Reservation reservation, ReservationStatus from) {
        eventPublisher.publish(new ReservationStatusChangedEvent(
                reservation.getId(),
                reservation.getOffer() == null ? null : reservation.getOffer().getId(),
                reservation.getBeneficiary() == null ? null : reservation.getBeneficiary().getId(),
                from, reservation.getStatus()));
        return reservation;
    }

    private Reservation findForBeneficiary(Long id, Long beneficiaryId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Reservation.class, id));
        if (!Objects.equals(reservation.getBeneficiary().getId(), beneficiaryId)) {
            throw new EntityNotFoundException(Reservation.class, id);
        }
        return reservation;
    }

    private Reservation findForOfferer(Long id, Long offererId) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Reservation.class, id));
        if (!Objects.equals(reservation.getOffer().getResource().getOfferer().getId(), offererId)) {
            throw new EntityNotFoundException(Reservation.class, id);
        }
        return reservation;
    }

    private String newPickupCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private static LocalDateTime earliest(LocalDateTime a, LocalDateTime b) {
        return b == null || a.isBefore(b) ? a : b;
    }
}
