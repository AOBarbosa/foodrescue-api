package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.validation.validators.OfferOrderBusinessValidator;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.domain.dtos.OfferOrderDTO;
import br.com.seudominio.perishrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.OfferOrder;
import br.com.seudominio.perishrescue.domain.enums.OfferOrderStatus;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.OfferOrderMapper;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OfferOrderRepository;

import jakarta.validation.Validator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that manages beneficiary reservations/purchases of surplus offers (UC09).
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
    private final BeneficiaryRepository beneficiaryRepository;
    private final OfferOrderBusinessValidator offerOrderValidator;

    /**
     * Constructor.
     *
     * @param repository          the offer order repository
     * @param mapper              the offer order mapper
     * @param validator           the bean validator
     * @param messageUtils        the message utils
     * @param offerService        the offer service for atomic quantity reduction
     * @param beneficiaryRepository  the beneficiary repository
     * @param offerOrderValidator the business validator for offer orders
     */
    public OfferOrderService(
            OfferOrderRepository repository,
            OfferOrderMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            OfferService offerService,
            BeneficiaryRepository beneficiaryRepository,
            OfferOrderBusinessValidator offerOrderValidator) {
        super(repository, mapper, validator, messageUtils);
        this.offerOrderRepository = repository;
        this.offerOrderMapper = mapper;
        this.offerService = offerService;
        this.beneficiaryRepository = beneficiaryRepository;
        this.offerOrderValidator = offerOrderValidator;
    }

    /**
     * Reserves units of an active offer for the authenticated beneficiary.
     *
     * @param request    the reservation request containing the offer id and desired quantity
     * @param beneficiaryId the authenticated beneficiary identifier
     * @return the created reservation DTO
     * @throws EntityNotFoundException        if the offer or beneficiary does not exist
     * @throws BusinessRuleViolationException if the request is invalid, the offer is not active,
     *                                        has expired, or does not have enough available quantity
     */
    public OfferOrderDTO reserve(ReserveOfferRequest request, Long beneficiaryId) {
        if (request == null || request.offerId() == null || request.quantity() == null) {
            throw new BusinessRuleViolationException("offerId and quantity must be informed");
        }
        return reserve(request.offerId(), beneficiaryId, request.quantity());
    }

    /**
     * Reserves units of an active offer for the authenticated beneficiary.
     *
     * @param offerId    the offer identifier
     * @param beneficiaryId the authenticated beneficiary identifier
     * @param quantity   the desired quantity
     * @return the created reservation DTO
     * @throws EntityNotFoundException        if the offer or beneficiary does not exist
     * @throws BusinessRuleViolationException if the offer is not active, has expired, the quantity
     *                                        is not positive, or there are not enough units left
     */
    public OfferOrderDTO reserve(Long offerId, Long beneficiaryId, int quantity) {
        if (offerId == null) {
            throw new BusinessRuleViolationException("offerId must be informed");
        }

        Beneficiary beneficiary = beneficiaryRepository.findById(beneficiaryId)
                .orElseThrow(() -> new EntityNotFoundException(Beneficiary.class, beneficiaryId));

        Offer offer = offerService.decreaseQuantity(offerId, quantity);

        OfferOrder order = OfferOrder.builder()
                .offer(offer)
                .beneficiary(beneficiary)
                .quantity(quantity)
                .status(OfferOrderStatus.RESERVED)
                .build();

        offerOrderValidator.validateOperation(order, BusinessOperation.CREATE);

        OfferOrder saved = offerOrderRepository.save(order);
        return offerOrderMapper.toDto(saved);
    }

    /**
     * Lists every active reservation made by the authenticated beneficiary, newest first.
     *
     * @param beneficiaryId the authenticated beneficiary identifier
     * @return the beneficiary's reservations
     */
    @Transactional(readOnly = true)
    public List<OfferOrderDTO> findAllForBeneficiary(Long beneficiaryId) {
        return offerOrderRepository.findByBeneficiaryIdAndActiveTrueOrderByCreationDateDesc(beneficiaryId)
                .stream()
                .map(offerOrderMapper::toDto)
                .toList();
    }

    /**
     * Fetches one reservation of the authenticated beneficiary.
     *
     * @param id         the reservation identifier
     * @param beneficiaryId the authenticated beneficiary identifier
     * @return the reservation DTO
     * @throws EntityNotFoundException if the reservation does not exist or belongs to another beneficiary
     */
    @Transactional(readOnly = true)
    public OfferOrderDTO findByIdForBeneficiary(Long id, Long beneficiaryId) {
        OfferOrder order = offerOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(OfferOrder.class, id));

        if (!order.getBeneficiary().getId().equals(beneficiaryId)) {
            throw new EntityNotFoundException(OfferOrder.class, id);
        }

        return offerOrderMapper.toDto(order);
    }
}
