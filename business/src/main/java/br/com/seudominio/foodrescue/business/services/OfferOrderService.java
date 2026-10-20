package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.validation.validators.OfferOrderBusinessValidator;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.foodrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.OfferOrder;
import br.com.seudominio.foodrescue.domain.enums.OfferOrderStatus;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.OfferOrderMapper;
import br.com.seudominio.foodrescue.persistence.repositories.ConsumerRepository;
import br.com.seudominio.foodrescue.persistence.repositories.OfferOrderRepository;

import jakarta.validation.Validator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that manages consumer reservations/purchases of surplus offers (UC09).
 *
 * <p>Reserving an offer atomically decrements the offer's available quantity
 * (via {@link OfferService#decreaseQuantity(Long, int)}, which relies on JPA
 * optimistic locking via {@code @Version} on {@link Offer}) and persists a
 * {@link OfferOrder} with status {@link OfferOrderStatus#RESERVED} in the same
 * transaction.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Service
@Transactional
public class OfferOrderService extends GenericService<OfferOrder, OfferOrderDTO> {

    private final OfferOrderRepository offerOrderRepository;
    private final OfferOrderMapper offerOrderMapper;
    private final OfferService offerService;
    private final ConsumerRepository consumerRepository;
    private final OfferOrderBusinessValidator offerOrderValidator;

    /**
     * Constructor.
     *
     * @param repository          the offer order repository
     * @param mapper              the offer order mapper
     * @param validator           the bean validator
     * @param messageUtils        the message utils
     * @param offerService        the offer service for atomic quantity reduction
     * @param consumerRepository  the consumer repository
     * @param offerOrderValidator the business validator for offer orders
     */
    public OfferOrderService(
            OfferOrderRepository repository,
            OfferOrderMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            OfferService offerService,
            ConsumerRepository consumerRepository,
            OfferOrderBusinessValidator offerOrderValidator) {
        super(repository, mapper, validator, messageUtils);
        this.offerOrderRepository = repository;
        this.offerOrderMapper = mapper;
        this.offerService = offerService;
        this.consumerRepository = consumerRepository;
        this.offerOrderValidator = offerOrderValidator;
    }

    /**
     * Reserves units of an active offer for the authenticated consumer.
     *
     * @param request    the reservation request containing the offer id and desired quantity
     * @param consumerId the authenticated consumer identifier
     * @return the created reservation DTO
     * @throws EntityNotFoundException        if the offer or consumer does not exist
     * @throws BusinessRuleViolationException if the request is invalid, the offer is not active,
     *                                        has expired, or does not have enough available quantity
     */
    public OfferOrderDTO reserve(ReserveOfferRequest request, Long consumerId) {
        if (request == null || request.offerId() == null || request.quantity() == null) {
            throw new BusinessRuleViolationException("offerId and quantity must be informed");
        }
        return reserve(request.offerId(), consumerId, request.quantity());
    }

    /**
     * Reserves units of an active offer for the authenticated consumer.
     *
     * @param offerId    the offer identifier
     * @param consumerId the authenticated consumer identifier
     * @param quantity   the desired quantity
     * @return the created reservation DTO
     * @throws EntityNotFoundException        if the offer or consumer does not exist
     * @throws BusinessRuleViolationException if the offer is not active, has expired, the quantity
     *                                        is not positive, or there are not enough units left
     */
    public OfferOrderDTO reserve(Long offerId, Long consumerId, int quantity) {
        if (offerId == null) {
            throw new BusinessRuleViolationException("offerId must be informed");
        }

        Consumer consumer = consumerRepository.findById(consumerId)
                .orElseThrow(() -> new EntityNotFoundException(Consumer.class, consumerId));

        Offer offer = offerService.decreaseQuantity(offerId, quantity);

        OfferOrder order = OfferOrder.builder()
                .offer(offer)
                .consumer(consumer)
                .quantity(quantity)
                .status(OfferOrderStatus.RESERVED)
                .build();

        offerOrderValidator.validateOperation(order, BusinessOperation.CREATE);

        OfferOrder saved = offerOrderRepository.save(order);
        return offerOrderMapper.toDto(saved);
    }

    /**
     * Lists every active reservation made by the authenticated consumer, newest first.
     *
     * @param consumerId the authenticated consumer identifier
     * @return the consumer's reservations
     */
    @Transactional(readOnly = true)
    public List<OfferOrderDTO> findAllForConsumer(Long consumerId) {
        return offerOrderRepository.findByConsumerIdAndActiveTrueOrderByCreationDateDesc(consumerId)
                .stream()
                .map(offerOrderMapper::toDto)
                .toList();
    }

    /**
     * Fetches one reservation of the authenticated consumer.
     *
     * @param id         the reservation identifier
     * @param consumerId the authenticated consumer identifier
     * @return the reservation DTO
     * @throws EntityNotFoundException if the reservation does not exist or belongs to another consumer
     */
    @Transactional(readOnly = true)
    public OfferOrderDTO findByIdForConsumer(Long id, Long consumerId) {
        OfferOrder order = offerOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(OfferOrder.class, id));

        if (!order.getConsumer().getId().equals(consumerId)) {
            throw new EntityNotFoundException(OfferOrder.class, id);
        }

        return offerOrderMapper.toDto(order);
    }
}
