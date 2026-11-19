package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.validation.validators.SaleBusinessValidator;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.domain.dtos.RegisterSaleDTO;
import br.com.seudominio.perishrescue.domain.dtos.SaleDTO;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.entities.Sale;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.SaleMapper;
import br.com.seudominio.perishrescue.persistence.repositories.SaleRepository;

import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service that provides operations for {@link Sale} entity (UC04).
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Service
@Transactional
public class SaleService extends GenericService<Sale, SaleDTO> {

    private final SaleRepository saleRepository;
    private final ResourceService resourceService;
    private final SaleBusinessValidator saleValidator;
    private final TimeProvider timeProvider;

    /**
     * Constructor.
     *
     * @param repository     the sale repository
     * @param resourceService the resource service for resource ownership and inventory operations
     * @param mapper         the sale mapper
     * @param validator      the bean validator
     * @param messageUtils   the message utils
     * @param saleValidator  the business validator for sale
     * @param timeProvider   the time provider
     */
    public SaleService(
            SaleRepository repository,
            ResourceService resourceService,
            SaleMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            SaleBusinessValidator saleValidator,
            TimeProvider timeProvider) {
        super(repository, mapper, validator, messageUtils);
        this.saleRepository = repository;
        this.resourceService = resourceService;
        this.saleValidator = saleValidator;
        this.timeProvider = timeProvider;
    }

    /**
     * Registers a new sale for a resource owned by the authenticated offerer.
     * Deducts the resource stock quantity via {@link ResourceService} and records the sale.
     *
     * @param dto             the sale registration data
     * @param offererId the authenticated offerer identifier
     * @return the registered sale DTO
     * @throws EntityNotFoundException        if the resource is not found or owned by another offerer
     * @throws BusinessRuleViolationException if the stock quantity is insufficient
     */
    public SaleDTO registerSale(RegisterSaleDTO dto, Long offererId) {
        PerishableResource resource = resourceService.deductStock(dto.resourceId(), offererId, dto.quantity());

        Money unitPrice = (dto.unitPrice() != null)
                ? Money.of(dto.unitPrice())
                : resource.getCurrentPrice();

        LocalDateTime soldAt = (dto.soldAt() != null)
                ? dto.soldAt()
                : timeProvider.now();

        Sale sale = Sale.builder()
                .resource(resource)
                .quantity(dto.quantity())
                .unitPrice(unitPrice)
                .soldAt(soldAt)
                .build();

        saleValidator.validateOperation(sale, BusinessOperation.CREATE);

        Sale saved = saleRepository.save(sale);
        return dtoMapper.toDto(saved);
    }

    /**
     * Finds active sales for a resource owned by the authenticated offerer,
     * optionally filtered by date range.
     *
     * @param resourceId       the resource identifier
     * @param offererId the authenticated offerer identifier
     * @param startDate       the start timestamp (optional)
     * @param endDate         the end timestamp (optional)
     * @return the list of sales DTOs
     * @throws EntityNotFoundException        if the resource is not found or owned by another offerer
     * @throws BusinessRuleViolationException if startDate is after endDate
     */
    @Transactional(readOnly = true)
    public List<SaleDTO> findSalesByResourceAndPeriod(
            Long resourceId,
            Long offererId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        resourceService.findResourceOwnedBy(resourceId, offererId);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleViolationException("startDate must not be after endDate");
        }

        return saleRepository.findByResourceAndPeriod(resourceId, startDate, endDate)
                .stream()
                .map(dtoMapper::toDto)
                .toList();
    }

    /**
     * Finds a sale by ID ensuring it belongs to the authenticated offerer.
     *
     * @param id              the sale identifier
     * @param offererId the authenticated offerer identifier
     * @return the sale DTO
     * @throws EntityNotFoundException if the sale is not found or belongs to another offerer
     */
    @Transactional(readOnly = true)
    public SaleDTO findByIdForOfferer(Long id, Long offererId) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Sale.class, id));

        if (!sale.getResource().getOfferer().getId().equals(offererId)) {
            throw new EntityNotFoundException(Sale.class, id);
        }

        return dtoMapper.toDto(sale);
    }
}
