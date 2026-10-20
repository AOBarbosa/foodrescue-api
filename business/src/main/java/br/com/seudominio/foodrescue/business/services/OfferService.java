package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.validation.validators.OfferBusinessValidator;
import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.domain.dtos.CreateOfferRequest;
import br.com.seudominio.foodrescue.domain.dtos.OfferDTO;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.OfferStatus;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.OfferMapper;
import br.com.seudominio.foodrescue.persistence.repositories.OfferRepository;

import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Service that publishes surplus as offers to consumers and closes them (UC08).
 *
 * <p>An offer is born in one of two ways, and both end at the same place:</p>
 * <ul>
 *   <li>automatically, when an establishment accepts a discount recommendation
 *       (UC07) &mdash; driven by a listener on the domain event, so UC07 never
 *       references this service;</li>
 *   <li>manually, when the establishment publishes one itself.</li>
 * </ul>
 *
 * <p>An offer is closed when its units run out, when the product runs out of
 * stock, or when the establishment closes it by hand. The invariant the issue
 * asks for &mdash; <em>an offer can never stay active without stock</em> &mdash;
 * is enforced by {@link #decreaseQuantity(Long, int)} and by
 * {@link #closeOffersForDepletedProduct(Long)}.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Service
@Transactional
public class OfferService extends GenericService<Offer, OfferDTO> {

    private final OfferRepository offerRepository;
    private final OfferMapper offerMapper;
    private final ProductService productService;
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
     * @param productService the product service, which owns product lookup
     * @param offerValidator the business validator for offers
     * @param timeProvider   the current date and time provider
     * @param closingTime    the end of the business day, shared with UC05 and UC07
     */
    public OfferService(
            OfferRepository repository,
            OfferMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ProductService productService,
            OfferBusinessValidator offerValidator,
            TimeProvider timeProvider,
            @Value("${app.forecast.closing-time:22:00}") String closingTime) {
        super(repository, mapper, validator, messageUtils);
        this.offerRepository = repository;
        this.offerMapper = mapper;
        this.productService = productService;
        this.offerValidator = offerValidator;
        this.timeProvider = timeProvider;
        this.closingTime = LocalTime.parse(closingTime);
    }

    /**
     * Publishes an offer manually.
     *
     * <p>Only the product is required: the price falls back to the product's
     * current price, the quantity to its whole stock, and the deadline to
     * today's closing time.</p>
     *
     * @param request         the offer data
     * @param establishmentId the authenticated establishment id
     * @return the published offer
     * @throws EntityNotFoundException        if the product does not exist or belongs to another establishment
     * @throws BusinessRuleViolationException if the product has no stock, the quantity exceeds the
     *                                        stock, or an active offer already exists for the product
     */
    public OfferDTO createOffer(CreateOfferRequest request, Long establishmentId) {
        if (request == null || request.productId() == null) {
            throw new BusinessRuleViolationException("productId must be informed");
        }

        Product product = productService.findProductOwnedBy(request.productId(), establishmentId);

        activeOfferOf(product.getId()).ifPresent(existing -> {
            throw new BusinessRuleViolationException(String.format(
                    "product %d already has an active offer (id %d); close it before publishing another",
                    product.getId(), existing.getId()));
        });

        return toDto(publish(
                product,
                request.discountedPrice() == null ? product.getCurrentPrice() : Money.of(request.discountedPrice()),
                request.availableQuantity(),
                request.expiresAt()));
    }

    /**
     * Publishes an offer for a product whose discount was just accepted (UC07).
     *
     * <p>Called by the listener of {@code DiscountRecommendationRespondedEvent},
     * already outside the transaction that answered the recommendation, so it
     * opens one of its own.</p>
     *
     * <p>If the product already has an active offer, that offer carries the old
     * price and is superseded: it is closed and a new one is published. This is
     * the opposite of the manual path, which refuses &mdash; here the system is
     * reacting to a price change and <em>must</em> replace the stale offer.</p>
     *
     * @param productId       the discounted product
     * @param establishmentId the establishment that owns it
     * @return the published offer, or empty if the product has no stock to offer
     * @throws EntityNotFoundException if the product does not exist or belongs to another establishment
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<OfferDTO> createFromAcceptedDiscount(Long productId, Long establishmentId) {
        Product product = productService.findProductOwnedBy(productId, establishmentId);

        if (product.getStockQuantity() <= 0) {
            return Optional.empty();
        }

        activeOfferOf(productId).ifPresent(this::close);

        return Optional.of(toDto(publish(product, product.getCurrentPrice(), null, null)));
    }

    /**
     * Closes an offer by hand, before its deadline.
     *
     * @param id              the offer id
     * @param establishmentId the authenticated establishment id
     * @return the closed offer
     * @throws EntityNotFoundException        if the offer does not exist or belongs to another establishment
     * @throws BusinessRuleViolationException if the offer is already closed
     */
    public OfferDTO closeOffer(Long id, Long establishmentId) {
        Offer offer = findOwnedBy(id, establishmentId);

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
     * with zero units. UC09 calls this when a consumer places an order.</p>
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
     * Closes every active offer of a product that ran out of stock.
     *
     * <p>Called by the listener of {@code ProductStockDepletedEvent}, already
     * outside the transaction that emptied the stock, so it opens one of its own.</p>
     *
     * @param productId the product that ran out
     * @return how many offers were closed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int closeOffersForDepletedProduct(Long productId) {
        List<Offer> openOffers = offerRepository
                .findByProductIdAndStatusAndActiveTrue(productId, OfferStatus.ACTIVE);

        openOffers.forEach(this::close);
        return openOffers.size();
    }

    /**
     * Lists every offer of an establishment, newest first.
     *
     * @param establishmentId the authenticated establishment id
     * @return the establishment's offers
     */
    @Transactional(readOnly = true)
    public List<OfferDTO> findAllForEstablishment(Long establishmentId) {
        return offerRepository.findByProductEstablishmentIdAndActiveTrueOrderByCreationDateDesc(establishmentId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Finds one offer of an establishment.
     *
     * @param id              the offer id
     * @param establishmentId the authenticated establishment id
     * @return the offer
     * @throws EntityNotFoundException if the offer does not exist or belongs to another establishment
     */
    @Transactional(readOnly = true)
    public OfferDTO findByIdForEstablishment(Long id, Long establishmentId) {
        return toDto(findOwnedBy(id, establishmentId));
    }

    /**
     * Lists the offers a consumer can still order: active, not expired and with
     * units left, highlighted ones first.
     *
     * @return the orderable offers
     */
    @Transactional(readOnly = true)
    public List<OfferDTO> findOrderable() {
        return offerRepository.findOrderable(timeProvider.now())
                .stream()
                .map(this::toDto)
                .toList();
    }

    private Offer publish(Product product, Money price, Integer requestedQuantity, LocalDateTime requestedDeadline) {
        if (product.getStockQuantity() <= 0) {
            throw new BusinessRuleViolationException(String.format(
                    "product %d has no stock to offer", product.getId()));
        }

        int quantity = requestedQuantity == null ? product.getStockQuantity() : requestedQuantity;
        if (quantity > product.getStockQuantity()) {
            throw new BusinessRuleViolationException(String.format(
                    "availableQuantity %d exceeds the product stock of %d",
                    quantity, product.getStockQuantity()));
        }

        LocalDateTime now = timeProvider.now();
        LocalDateTime deadline = requestedDeadline == null ? now.toLocalDate().atTime(closingTime) : requestedDeadline;

        Offer offer = Offer.builder()
                .product(product)
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

    private Optional<Offer> activeOfferOf(Long productId) {
        return offerRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                productId, OfferStatus.ACTIVE);
    }

    private OfferDTO toDto(Offer offer) {
        return offerMapper.toDto(offer);
    }

    private Offer findOwnedBy(Long id, Long establishmentId) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Offer.class, id));

        if (!offer.getProduct().getEstablishment().getId().equals(establishmentId)) {
            throw new EntityNotFoundException(Offer.class, id);
        }
        return offer;
    }
}
