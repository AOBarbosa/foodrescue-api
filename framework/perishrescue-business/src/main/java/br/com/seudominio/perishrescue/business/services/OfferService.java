package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.event.DomainEventPublisher;
import br.com.seudominio.perishrescue.business.event.OfferStatusChangedEvent;
import br.com.seudominio.perishrescue.business.pricing.PriceCalculator;
import br.com.seudominio.perishrescue.business.ranking.OfferRankingPolicy;
import br.com.seudominio.perishrescue.business.validation.validators.OfferBusinessValidator;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.domain.dtos.CreateOfferRequest;
import br.com.seudominio.perishrescue.domain.dtos.OfferDTO;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.OfferMapper;
import br.com.seudominio.perishrescue.persistence.repositories.OfferRepository;

import jakarta.validation.Validator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service that publishes surplus as offers to beneficiaries and drives each
 * offer through the kernel's lifecycle state machine (UC08, frozen spot).
 *
 * <p>An offer is born in one of two ways, and both end at the same place:</p>
 * <ul>
 *   <li>automatically, when an offerer accepts a discount recommendation
 *       (UC07) &mdash; driven by a listener on the domain event, so UC07 never
 *       references this service;</li>
 *   <li>manually, when the offerer publishes one itself.</li>
 * </ul>
 *
 * <p>A new offer is {@link OfferStatus#AVAILABLE}, runs until its resource's
 * {@code criticalDeadline} unless the offerer sets an earlier one, and is
 * priced by the {@link PriceCalculator} (hot spot 2). It becomes
 * {@link OfferStatus#SOLD_OUT} when every unit is reserved (and available
 * again if a reservation gives units back), {@link OfferStatus#EXPIRED} when
 * its deadline passes, and {@link OfferStatus#WITHDRAWN} when the offerer
 * closes it, a new price supersedes it, or the resource runs out of stock.
 * Every transition publishes an {@link OfferStatusChangedEvent}.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Service
@Transactional
public class OfferService extends GenericService<Offer, OfferDTO> {

    private final OfferRepository offerRepository;
    private final OfferMapper offerMapper;
    private final ResourceService resourceService;
    private final OfferBusinessValidator offerValidator;
    private final TimeProvider timeProvider;
    private final PriceCalculator priceCalculator;
    private final DomainEventPublisher eventPublisher;

    /**
     * Constructor.
     *
     * @param repository      the offer repository
     * @param mapper          the offer mapper
     * @param validator       the bean validator
     * @param messageUtils    the message utils
     * @param resourceService the resource service, which owns resource lookup
     * @param offerValidator  the business validator for offers
     * @param timeProvider    the current date and time provider
     * @param priceCalculator the kernel's pricing rules around the instance's pricing policy
     * @param eventPublisher  the domain event publisher
     */
    public OfferService(
            OfferRepository repository,
            OfferMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ResourceService resourceService,
            OfferBusinessValidator offerValidator,
            TimeProvider timeProvider,
            PriceCalculator priceCalculator,
            DomainEventPublisher eventPublisher) {
        super(repository, mapper, validator, messageUtils);
        this.offerRepository = repository;
        this.offerMapper = mapper;
        this.resourceService = resourceService;
        this.offerValidator = offerValidator;
        this.timeProvider = timeProvider;
        this.priceCalculator = priceCalculator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes an offer manually.
     *
     * <p>Only the resource is required: the asking price falls back to the
     * resource's current price, the quantity to its whole stock, and the
     * deadline to the resource's critical deadline.</p>
     *
     * @param request   the offer data
     * @param offererId the authenticated offerer id
     * @return the published offer
     * @throws EntityNotFoundException        if the resource does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if the resource has no stock, the quantity exceeds the
     *                                        stock, or an open offer already exists for the resource
     */
    public OfferDTO createOffer(CreateOfferRequest request, Long offererId) {
        if (request == null || request.resourceId() == null) {
            throw new BusinessRuleViolationException("resourceId must be informed");
        }

        PerishableResource resource = resourceService.findResourceOwnedBy(request.resourceId(), offererId);

        openOfferOf(resource.getId()).ifPresent(existing -> {
            throw new BusinessRuleViolationException(String.format(
                    "resource %d already has an open offer (id %d); close it before publishing another",
                    resource.getId(), existing.getId()));
        });

        return toDto(publish(
                resource,
                request.discountedPrice() == null ? resource.getCurrentPrice() : Money.of(request.discountedPrice()),
                request.availableQuantity(),
                request.expiresAt()));
    }

    /**
     * Publishes an offer for a resource whose discount was just accepted (UC07).
     *
     * <p>Called by the listener of {@code DiscountRecommendationRespondedEvent},
     * already outside the transaction that answered the recommendation, so it
     * opens one of its own.</p>
     *
     * <p>If the resource already has an open offer, that offer carries the old
     * price and is superseded: it is withdrawn and a new one is published.
     * This is the opposite of the manual path, which refuses &mdash; here the
     * system is reacting to a price change and <em>must</em> replace the stale
     * offer.</p>
     *
     * @param resourceId the discounted resource
     * @param offererId  the offerer that owns it
     * @return the published offer, or empty if the resource has no stock to offer
     * @throws EntityNotFoundException if the resource does not exist or belongs to another offerer
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OfferDTO> createFromAcceptedDiscount(Long resourceId, Long offererId) {
        PerishableResource resource = resourceService.findResourceOwnedBy(resourceId, offererId);

        if (resource.getStockQuantity() <= 0) {
            return Optional.empty();
        }

        openOfferOf(resourceId).ifPresent(offer -> move(offer, OfferStatus.WITHDRAWN));

        return Optional.of(toDto(publish(resource, resource.getCurrentPrice(), null, null)));
    }

    /**
     * Marks an offer as priority/featured for beneficiaries (UC11).
     *
     * @param offerId   the offer id
     * @param offererId the authenticated offerer id
     * @return the updated offer
     * @throws EntityNotFoundException        if the offer does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if the offer is not open
     */
    public OfferDTO markAsPriority(Long offerId, Long offererId) {
        Offer offer = findOwnedBy(offerId, offererId);
        requireOpen(offer);

        offer.markAsPriority();
        return toDto(offerRepository.save(offer));
    }

    /**
     * Marks the open offer of a resource as priority, or publishes one already
     * flagged as priority if none exists (UC11).
     *
     * <p>Called automatically when a SURPLUS_DESTINATION recommendation is accepted.</p>
     *
     * @param resourceId the resource id
     * @param offererId  the offerer id
     * @return the updated or created offer, or empty if no stock left
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OfferDTO> markOrPublishPriorityOfferForResource(Long resourceId, Long offererId) {
        PerishableResource resource = resourceService.findResourceOwnedBy(resourceId, offererId);

        if (resource.getStockQuantity() <= 0) {
            return Optional.empty();
        }

        Optional<Offer> existingOpen = openOfferOf(resourceId);
        if (existingOpen.isPresent()) {
            Offer offer = existingOpen.get();
            offer.markAsPriority();
            return Optional.of(toDto(offerRepository.save(offer)));
        }

        Offer newOffer = publish(resource, resource.getCurrentPrice(), null, null);
        newOffer.markAsPriority();
        return Optional.of(toDto(offerRepository.save(newOffer)));
    }

    /**
     * Closes an offer by hand, before its deadline ({@link OfferStatus#WITHDRAWN}).
     *
     * @param id        the offer id
     * @param offererId the authenticated offerer id
     * @return the closed offer
     * @throws EntityNotFoundException        if the offer does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if the offer is already closed
     */
    public OfferDTO closeOffer(Long id, Long offererId) {
        Offer offer = findOwnedBy(id, offererId);
        requireOpen(offer);

        return toDto(move(offer, OfferStatus.WITHDRAWN));
    }

    /**
     * Takes units out of an offer for a reservation, moving it to
     * {@link OfferStatus#SOLD_OUT} when nothing is left. Relies on the
     * offer's {@code @Version} for concurrent reservations.
     *
     * @param offerId  the offer id
     * @param quantity how many units to take
     * @return the updated offer
     * @throws EntityNotFoundException        if the offer does not exist
     * @throws BusinessRuleViolationException if the offer is not available, has expired, the quantity
     *                                        is not positive, or there are not enough units left
     */
    public Offer decreaseQuantity(Long offerId, int quantity) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException(Offer.class, offerId));

        if (quantity <= 0) {
            throw new BusinessRuleViolationException("quantity must be greater than zero");
        }
        if (offer.getStatus() != OfferStatus.AVAILABLE) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d is not available: current status is %s", offerId, offer.getStatus()));
        }
        if (!offer.getExpiresAt().isAfter(timeProvider.now())) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d has expired at %s", offerId, offer.getExpiresAt()));
        }

        OfferStatus before = offer.getStatus();
        offer.takeUnits(quantity);
        Offer saved = offerRepository.save(offer);
        announceIfChanged(saved, before);
        return saved;
    }

    /**
     * Gives back the units of an expired or cancelled reservation. An open
     * offer still within its deadline takes them back (a sold-out one becomes
     * available again); otherwise they are simply not offered anymore.
     *
     * @param offer    the reserved offer
     * @param quantity the units to give back
     * @return {@code true} if the offer took the units back
     */
    public boolean returnUnits(Offer offer, int quantity) {
        if (!offer.getExpiresAt().isAfter(timeProvider.now())) {
            return false;
        }
        OfferStatus before = offer.getStatus();
        boolean returned = offer.returnUnits(quantity);
        if (returned) {
            announceIfChanged(offerRepository.save(offer), before);
        }
        return returned;
    }

    /**
     * Withdraws every open offer of a resource that ran out of stock.
     *
     * <p>Called by the listener of {@code ResourceStockDepletedEvent}, already
     * outside the transaction that emptied the stock, so it opens one of its own.</p>
     *
     * @param resourceId the resource that ran out
     * @return how many offers were withdrawn
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int closeOffersForDepletedResource(Long resourceId) {
        List<Offer> openOffers = offerRepository
                .findByResourceIdAndStatusInAndActiveTrue(resourceId, OfferStatus.openStatuses());

        openOffers.forEach(offer -> move(offer, OfferStatus.WITHDRAWN));
        return openOffers.size();
    }

    /**
     * Expires every open offer whose deadline has passed. Run periodically by
     * the kernel's {@code ExpirationMonitor}.
     *
     * @return how many offers expired
     */
    public int expireOverdueOffers() {
        List<Offer> overdue = offerRepository.findByStatusInAndExpiresAtLessThanEqualAndActiveTrue(
                OfferStatus.openStatuses(), timeProvider.now());

        overdue.forEach(offer -> move(offer, OfferStatus.EXPIRED));
        return overdue.size();
    }

    /**
     * Reapplies the pricing rules to every open offer still within its
     * deadline, since the discount grows as the deadline approaches. Prices
     * only go down. Run periodically by the kernel's {@code RepricingScheduler}.
     *
     * @return how many offers got a lower price
     */
    public int repriceOpenOffers() {
        LocalDateTime now = timeProvider.now();
        int repriced = 0;
        for (Offer offer : offerRepository.findByStatusInAndExpiresAtGreaterThanAndActiveTrue(
                OfferStatus.openStatuses(), now)) {
            Money newPrice = priceCalculator.offerPrice(offer.getResource(), offer.getDiscountedPrice(), now);
            if (offer.getDiscountedPrice().isGreaterThan(newPrice)) {
                offer.setDiscountedPrice(newPrice);
                offerRepository.save(offer);
                repriced++;
            }
        }
        return repriced;
    }

    /**
     * Lists every offer of an offerer, newest first.
     *
     * @param offererId the authenticated offerer id
     * @return the offerer's offers
     */
    @Transactional(readOnly = true)
    public List<OfferDTO> findAllForOfferer(Long offererId) {
        return offerRepository.findByResourceOffererIdAndActiveTrueOrderByCreationDateDesc(offererId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Finds one offer of an offerer.
     *
     * @param id        the offer id
     * @param offererId the authenticated offerer id
     * @return the offer
     * @throws EntityNotFoundException if the offer does not exist or belongs to another offerer
     */
    @Transactional(readOnly = true)
    public OfferDTO findByIdForOfferer(Long id, Long offererId) {
        return toDto(findOwnedBy(id, offererId));
    }

    /**
     * Lists the offers a beneficiary can still reserve: available, not expired
     * and with units left, highlighted ones first.
     *
     * @return the orderable offers
     */
    @Transactional(readOnly = true)
    public List<OfferDTO> findOrderable() {
        List<Offer> offers = new ArrayList<>(offerRepository.findOrderable(timeProvider.now()));
        offers.sort(new OfferRankingPolicy());
        return offers.stream()
                .map(this::toDto)
                .toList();
    }

    private Offer publish(PerishableResource resource, Money askingPrice, Integer requestedQuantity,
                          LocalDateTime requestedDeadline) {
        if (resource.getStockQuantity() <= 0) {
            throw new BusinessRuleViolationException(String.format(
                    "resource %d has no stock to offer", resource.getId()));
        }

        int quantity = requestedQuantity == null ? resource.getStockQuantity() : requestedQuantity;
        if (quantity > resource.getStockQuantity()) {
            throw new BusinessRuleViolationException(String.format(
                    "availableQuantity %d exceeds the resource stock of %d",
                    quantity, resource.getStockQuantity()));
        }

        LocalDateTime now = timeProvider.now();
        LocalDateTime deadline = requestedDeadline == null ? resource.criticalDeadline(now) : requestedDeadline;

        Offer offer = Offer.builder()
                .resource(resource)
                .discountedPrice(askingPrice)
                .availableQuantity(quantity)
                .expiresAt(deadline)
                .build();

        // validate what the offerer asked for (e.g. never above the original price) before
        // the pricing policy lowers it
        offerValidator.validateOperation(offer, BusinessOperation.CREATE);
        offer.setDiscountedPrice(priceCalculator.offerPrice(resource, askingPrice, now));

        return offerRepository.save(offer);
    }

    private Offer move(Offer offer, OfferStatus target) {
        OfferStatus before = offer.getStatus();
        offer.transitionTo(target);
        Offer saved = offerRepository.save(offer);
        announceIfChanged(saved, before);
        return saved;
    }

    private void announceIfChanged(Offer offer, OfferStatus before) {
        if (offer.getStatus() != before) {
            eventPublisher.publish(new OfferStatusChangedEvent(
                    offer.getId(), offer.getResource() == null ? null : offer.getResource().getId(),
                    before, offer.getStatus()));
        }
    }

    private static void requireOpen(Offer offer) {
        if (offer.getStatus() == null || !offer.getStatus().isOpen()) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d is not open: current status is %s", offer.getId(), offer.getStatus()));
        }
    }

    private Optional<Offer> openOfferOf(Long resourceId) {
        return offerRepository.findFirstByResourceIdAndStatusInAndActiveTrueOrderByCreationDateDesc(
                resourceId, OfferStatus.openStatuses());
    }

    private OfferDTO toDto(Offer offer) {
        return offerMapper.toDto(offer);
    }

    private Offer findOwnedBy(Long id, Long offererId) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Offer.class, id));

        if (!offer.getResource().getOfferer().getId().equals(offererId)) {
            throw new EntityNotFoundException(Offer.class, id);
        }
        return offer;
    }
}
