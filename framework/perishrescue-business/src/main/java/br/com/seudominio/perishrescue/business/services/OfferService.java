package br.com.seudominio.perishrescue.business.services;

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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import br.com.seudominio.perishrescue.business.ranking.OfferRankingPolicy;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Service that publishes surplus as offers to beneficiaries and closes them (UC08).
 *
 * <p>An offer is born in one of two ways, and both end at the same place:</p>
 * <ul>
 *   <li>automatically, when an offerer accepts a discount recommendation
 *       (UC07) &mdash; driven by a listener on the domain event, so UC07 never
 *       references this service;</li>
 *   <li>manually, when the offerer publishes one itself.</li>
 * </ul>
 *
 * <p>An offer is closed when its units run out, when the resource runs out of
 * stock, or when the offerer closes it by hand. The invariant the issue
 * asks for &mdash; <em>an offer can never stay active without stock</em> &mdash;
 * is enforced by {@link #decreaseQuantity(Long, int)} and by
 * {@link #closeOffersForDepletedResource(Long)}.</p>
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
    private final LocalTime closingTime;

    /**
     * Constructor.
     *
     * @param repository     the offer repository
     * @param mapper         the offer mapper
     * @param validator      the bean validator
     * @param messageUtils   the message utils
     * @param resourceService the resource service, which owns resource lookup
     * @param offerValidator the business validator for offers
     * @param timeProvider   the current date and time provider
     * @param closingTime    the end of the business day, shared with UC05 and UC07
     */
    public OfferService(
            OfferRepository repository,
            OfferMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ResourceService resourceService,
            OfferBusinessValidator offerValidator,
            TimeProvider timeProvider,
            @Value("${app.forecast.closing-time:22:00}") String closingTime) {
        super(repository, mapper, validator, messageUtils);
        this.offerRepository = repository;
        this.offerMapper = mapper;
        this.resourceService = resourceService;
        this.offerValidator = offerValidator;
        this.timeProvider = timeProvider;
        this.closingTime = LocalTime.parse(closingTime);
    }

    /**
     * Publishes an offer manually.
     *
     * <p>Only the resource is required: the price falls back to the resource's
     * current price, the quantity to its whole stock, and the deadline to
     * today's closing time.</p>
     *
     * @param request         the offer data
     * @param offererId the authenticated offerer id
     * @return the published offer
     * @throws EntityNotFoundException        if the resource does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if the resource has no stock, the quantity exceeds the
     *                                        stock, or an active offer already exists for the resource
     */
    public OfferDTO createOffer(CreateOfferRequest request, Long offererId) {
        if (request == null || request.resourceId() == null) {
            throw new BusinessRuleViolationException("resourceId must be informed");
        }

        PerishableResource resource = resourceService.findResourceOwnedBy(request.resourceId(), offererId);

        activeOfferOf(resource.getId()).ifPresent(existing -> {
            throw new BusinessRuleViolationException(String.format(
                    "resource %d already has an active offer (id %d); close it before publishing another",
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
     * <p>If the resource already has an active offer, that offer carries the old
     * price and is superseded: it is closed and a new one is published. This is
     * the opposite of the manual path, which refuses &mdash; here the system is
     * reacting to a price change and <em>must</em> replace the stale offer.</p>
     *
     * @param resourceId       the discounted resource
     * @param offererId the offerer that owns it
     * @return the published offer, or empty if the resource has no stock to offer
     * @throws EntityNotFoundException if the resource does not exist or belongs to another offerer
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OfferDTO> createFromAcceptedDiscount(Long resourceId, Long offererId) {
        PerishableResource resource = resourceService.findResourceOwnedBy(resourceId, offererId);

        if (resource.getStockQuantity() <= 0) {
            return Optional.empty();
        }

        activeOfferOf(resourceId).ifPresent(this::close);

        return Optional.of(toDto(publish(resource, resource.getCurrentPrice(), null, null)));
    }

    /**
     * Closes an offer by hand, before its deadline.
     *
     * @param id              the offer id
     * @param offererId the authenticated offerer id
     * @return the closed offer
     * @throws EntityNotFoundException        if the offer does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if the offer is already closed
     */
    /**
     * Marks an offer as priority/featured for beneficiaries (UC11).
     *
     * @param offerId         the offer id
     * @param offererId the authenticated offerer id
     * @return the updated offer
     * @throws EntityNotFoundException        if the offer does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if the offer is not active
     */
    public OfferDTO markAsPriority(Long offerId, Long offererId) {
        Offer offer = findOwnedBy(offerId, offererId);

        if (offer.getStatus() != OfferStatus.ACTIVE) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d is not active: current status is %s", offerId, offer.getStatus()));
        }

        offer.markAsPriority();
        return toDto(offerRepository.save(offer));
    }

    /**
     * Marks the active offer of a resource as priority, or creates an active offer
     * already flagged as priority if none exists (UC11).
     *
     * <p>Called automatically when a SURPLUS_DESTINATION recommendation is accepted.</p>
     *
     * @param resourceId       the resource id
     * @param offererId the offerer id
     * @return the updated or created offer, or empty if no stock left
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OfferDTO> markOrPublishPriorityOfferForResource(Long resourceId, Long offererId) {
        PerishableResource resource = resourceService.findResourceOwnedBy(resourceId, offererId);

        if (resource.getStockQuantity() <= 0) {
            return Optional.empty();
        }

        Optional<Offer> existingActive = activeOfferOf(resourceId);
        if (existingActive.isPresent()) {
            Offer offer = existingActive.get();
            offer.markAsPriority();
            return Optional.of(toDto(offerRepository.save(offer)));
        }

        Offer newOffer = publish(resource, resource.getCurrentPrice(), null, null);
        newOffer.markAsPriority();
        return Optional.of(toDto(offerRepository.save(newOffer)));
    }
    public OfferDTO closeOffer(Long id, Long offererId) {
        Offer offer = findOwnedBy(id, offererId);

        if (offer.getStatus() != OfferStatus.ACTIVE) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d is not active: current status is %s", id, offer.getStatus()));
        }

        return toDto(close(offer));
    }

    /**
     * Takes units out of an offer, closing it when nothing is left.
     *
     * <p>This is the invariant the issue asks for: an offer never stays active
     * with zero units. UC09 calls this when a beneficiary places an order.</p>
     *
     * @param offerId  the offer id
     * @param quantity how many units to take
     * @return the updated offer
     * @throws EntityNotFoundException        if the offer does not exist
     * @throws BusinessRuleViolationException if the offer is not active, has expired, the quantity
     *                                        is not positive, or there are not enough units left
     */
    public Offer decreaseQuantity(Long offerId, int quantity) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException(Offer.class, offerId));

        if (quantity <= 0) {
            throw new BusinessRuleViolationException("quantity must be greater than zero");
        }
        if (offer.getStatus() != OfferStatus.ACTIVE) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d is not active: current status is %s", offerId, offer.getStatus()));
        }
        if (!offer.getExpiresAt().isAfter(timeProvider.now())) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d has expired at %s", offerId, offer.getExpiresAt()));
        }
        if (offer.getAvailableQuantity() < quantity) {
            throw new BusinessRuleViolationException(String.format(
                    "offer %d has only %d units left", offerId, offer.getAvailableQuantity()));
        }

        offer.setAvailableQuantity(offer.getAvailableQuantity() - quantity);
        if (offer.getAvailableQuantity() == 0) {
            offer.setStatus(OfferStatus.CLOSED);
        }

        return offerRepository.save(offer);
    }

    /**
     * Closes every active offer of a resource that ran out of stock.
     *
     * <p>Called by the listener of {@code ResourceStockDepletedEvent}, already
     * outside the transaction that emptied the stock, so it opens one of its own.</p>
     *
     * @param resourceId the resource that ran out
     * @return how many offers were closed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int closeOffersForDepletedResource(Long resourceId) {
        List<Offer> openOffers = offerRepository
                .findByResourceIdAndStatusAndActiveTrue(resourceId, OfferStatus.ACTIVE);

        openOffers.forEach(this::close);
        return openOffers.size();
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
     * @param id              the offer id
     * @param offererId the authenticated offerer id
     * @return the offer
     * @throws EntityNotFoundException if the offer does not exist or belongs to another offerer
     */
    @Transactional(readOnly = true)
    public OfferDTO findByIdForOfferer(Long id, Long offererId) {
        return toDto(findOwnedBy(id, offererId));
    }

    /**
     * Lists the offers a beneficiary can still order: active, not expired and with
     * units left, highlighted ones first.
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

    private Offer publish(PerishableResource resource, Money price, Integer requestedQuantity, LocalDateTime requestedDeadline) {
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
        LocalDateTime deadline = requestedDeadline == null ? now.toLocalDate().atTime(closingTime) : requestedDeadline;

        Offer offer = Offer.builder()
                .resource(resource)
                .discountedPrice(price)
                .availableQuantity(quantity)
                .expiresAt(deadline)
                .build();

        offerValidator.validateOperation(offer, BusinessOperation.CREATE);

        return offerRepository.save(offer);
    }

    private Offer close(Offer offer) {
        offer.setStatus(OfferStatus.CLOSED);
        return offerRepository.save(offer);
    }

    private Optional<Offer> activeOfferOf(Long resourceId) {
        return offerRepository.findFirstByResourceIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                resourceId, OfferStatus.ACTIVE);
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
