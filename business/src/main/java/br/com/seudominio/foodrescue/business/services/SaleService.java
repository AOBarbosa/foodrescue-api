package br.com.seudominio.foodrescue.business.services;

import br.com.seudominio.foodrescue.business.validation.validators.SaleBusinessValidator;
import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.domain.dtos.RegisterSaleDTO;
import br.com.seudominio.foodrescue.domain.dtos.SaleDTO;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.SaleMapper;
import br.com.seudominio.foodrescue.persistence.repositories.SaleRepository;

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
    private final ProductService productService;
    private final SaleBusinessValidator saleValidator;
    private final TimeProvider timeProvider;

    /**
     * Constructor.
     *
     * @param repository     the sale repository
     * @param productService the product service for product ownership and inventory operations
     * @param mapper         the sale mapper
     * @param validator      the bean validator
     * @param messageUtils   the message utils
     * @param saleValidator  the business validator for sale
     * @param timeProvider   the time provider
     */
    public SaleService(
            SaleRepository repository,
            ProductService productService,
            SaleMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            SaleBusinessValidator saleValidator,
            TimeProvider timeProvider) {
        super(repository, mapper, validator, messageUtils);
        this.saleRepository = repository;
        this.productService = productService;
        this.saleValidator = saleValidator;
        this.timeProvider = timeProvider;
    }

    /**
     * Registers a new sale for a product owned by the authenticated establishment.
     * Deducts the product stock quantity via {@link ProductService} and records the sale.
     *
     * @param dto             the sale registration data
     * @param establishmentId the authenticated establishment identifier
     * @return the registered sale DTO
     * @throws EntityNotFoundException        if the product is not found or owned by another establishment
     * @throws BusinessRuleViolationException if the stock quantity is insufficient
     */
    public SaleDTO registerSale(RegisterSaleDTO dto, Long establishmentId) {
        Product product = productService.deductStock(dto.productId(), establishmentId, dto.quantity());

        Money unitPrice = (dto.unitPrice() != null)
                ? Money.of(dto.unitPrice())
                : product.getCurrentPrice();

        LocalDateTime soldAt = (dto.soldAt() != null)
                ? dto.soldAt()
                : timeProvider.now();

        Sale sale = Sale.builder()
                .product(product)
                .quantity(dto.quantity())
                .unitPrice(unitPrice)
                .soldAt(soldAt)
                .build();

        saleValidator.validateOperation(sale, BusinessOperation.CREATE);

        Sale saved = saleRepository.save(sale);
        return dtoMapper.toDto(saved);
    }

    /**
     * Finds active sales for a product owned by the authenticated establishment,
     * optionally filtered by date range.
     *
     * @param productId       the product identifier
     * @param establishmentId the authenticated establishment identifier
     * @param startDate       the start timestamp (optional)
     * @param endDate         the end timestamp (optional)
     * @return the list of sales DTOs
     * @throws EntityNotFoundException        if the product is not found or owned by another establishment
     * @throws BusinessRuleViolationException if startDate is after endDate
     */
    @Transactional(readOnly = true)
    public List<SaleDTO> findSalesByProductAndPeriod(
            Long productId,
            Long establishmentId,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        productService.findProductOwnedBy(productId, establishmentId);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessRuleViolationException("startDate must not be after endDate");
        }

        return saleRepository.findByProductAndPeriod(productId, startDate, endDate)
                .stream()
                .map(dtoMapper::toDto)
                .toList();
    }

    /**
     * Finds a sale by ID ensuring it belongs to the authenticated establishment.
     *
     * @param id              the sale identifier
     * @param establishmentId the authenticated establishment identifier
     * @return the sale DTO
     * @throws EntityNotFoundException if the sale is not found or belongs to another establishment
     */
    @Transactional(readOnly = true)
    public SaleDTO findByIdForEstablishment(Long id, Long establishmentId) {
        Sale sale = saleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(Sale.class, id));

        if (!sale.getProduct().getEstablishment().getId().equals(establishmentId)) {
            throw new EntityNotFoundException(Sale.class, id);
        }

        return dtoMapper.toDto(sale);
    }
}
