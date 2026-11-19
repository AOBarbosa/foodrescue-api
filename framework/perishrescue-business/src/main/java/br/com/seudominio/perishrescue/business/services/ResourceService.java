package br.com.seudominio.perishrescue.business.services;

import br.com.seudominio.perishrescue.business.event.DomainEventPublisher;
import br.com.seudominio.perishrescue.business.event.ResourceStockDepletedEvent;
import br.com.seudominio.perishrescue.business.validation.validators.ResourceBusinessValidator;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.dtos.ResourceDTO;
import br.com.seudominio.perishrescue.domain.dtos.ResourceInventoryUpdateDTO;
import br.com.seudominio.perishrescue.domain.dtos.UpdateResourceInventoryRequest;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.ResourceMapper;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ResourceRepository;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service that provides operations for {@link PerishableResource} entity.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Service
@Transactional
public class ResourceService extends GenericService<PerishableResource, ResourceDTO> {

    private final ResourceRepository resourceRepository;
    private final OffererRepository offererRepository;
    private final ResourceMapper resourceMapper;
    private final ResourceBusinessValidator resourceValidator;
    private final TimeProvider timeProvider;
    private final DomainEventPublisher eventPublisher;

    /**
     * Constructor.
     *
     * @param repository                the resource repository
     * @param offererRepository   the offerer repository
     * @param mapper                    the resource mapper
     * @param validator                 the bean validator
     * @param messageUtils              the message utils
     * @param resourceValidator          the business validator for the entity
     * @param timeProvider              the current date and time provider
     * @param eventPublisher            the domain event publisher, used to announce a depleted stock
     */
    public ResourceService(
            ResourceRepository repository,
            OffererRepository offererRepository,
            ResourceMapper mapper,
            Validator validator,
            MessageUtils messageUtils,
            ResourceBusinessValidator resourceValidator,
            TimeProvider timeProvider,
            DomainEventPublisher eventPublisher) {
        super(repository, mapper, validator, messageUtils);
        this.resourceRepository = repository;
        this.offererRepository = offererRepository;
        this.resourceMapper = mapper;
        this.resourceValidator = resourceValidator;
        this.timeProvider = timeProvider;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Registers a new resource of any concrete type for an offerer.
     *
     * <p>Frozen part of the registration flow: the instance builds the
     * resource (through its {@code ResourceTypeMapper}); the kernel attaches
     * the owner, validates and persists it. The current price starts equal to
     * the original price and the stock at zero.</p>
     *
     * @param resource  the new, not yet persisted resource
     * @param offererId the offerer id
     * @param <R>       the concrete resource type
     * @return the registered resource
     * @throws EntityNotFoundException if the offerer does not exist
     * @throws ValidationException     if the entity violates a constraint
     */
    public <R extends PerishableResource> R register(R resource, Long offererId) {
        Offerer offerer = offererRepository.findById(offererId)
                .orElseThrow(() -> new EntityNotFoundException(Offerer.class, offererId));
        resource.setOfferer(offerer);
        if (resource.getCurrentPrice() == null) {
            resource.setCurrentPrice(resource.getOriginalPrice());
        }

        resourceValidator.validateOperation(resource, BusinessOperation.CREATE);

        return resourceRepository.save(resource);
    }

    /**
     * Finds all active resources for an authenticated offerer.
     * @param offererId the offerer id
     * @return the list of resources
     */
    public List<ResourceDTO> findAllForOfferer(Long offererId) {
        return findAllForOfferer(offererId, PerishableResource.class)
                .stream()
                .map(resourceMapper::toDto)
                .toList();
    }

    /**
     * Finds all active resources of one concrete type for an authenticated offerer.
     *
     * @param offererId the offerer id
     * @param type      the concrete resource type
     * @param <R>       the concrete resource type
     * @return the offerer's resources of that type
     * @throws EntityNotFoundException if the offerer does not exist
     */
    public <R extends PerishableResource> List<R> findAllForOfferer(Long offererId, Class<R> type) {
        if (!offererRepository.existsById(offererId)) {
            throw new EntityNotFoundException(Offerer.class, offererId);
        }

        return resourceRepository.findAllByOffererIdAndActiveTrue(offererId)
                .stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    /**
     * Finds a resource of one concrete type owned by an offerer.
     *
     * @param id        the resource id
     * @param offererId the offerer id
     * @param type      the concrete resource type
     * @param <R>       the concrete resource type
     * @return the resource
     * @throws EntityNotFoundException if the resource does not exist, belongs to another
     *                                 offerer or is of another type
     */
    public <R extends PerishableResource> R findResourceOwnedBy(Long id, Long offererId, Class<R> type) {
        PerishableResource resource = findResourceOwnedBy(id, offererId);
        if (!type.isInstance(resource)) {
            throw new EntityNotFoundException(PerishableResource.class, id);
        }
        return type.cast(resource);
    }

    /**
     * Finds a resource for an authenticated offerer.
     *
     * @param id the resource id
     * @param offererId the offerer id
     * @return the resource
     */
    public ResourceDTO findByIdForOfferer(Long id, Long offererId) {
        return resourceMapper.toDto(findResourceOwnedBy(id, offererId));
    }

    /**
     * Partially updates a resource's stock quantity and/or expiration date.
     *
     * @param id the resource id
     * @param offererId the authenticated offerer id
     * @param request the inventory fields to update
     * @return the updated resource and the expiration warning flag
     * @throws EntityNotFoundException if the resource does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if no field is informed or the stock quantity is negative
     */
    public ResourceInventoryUpdateDTO updateInventory(
            Long id,
            Long offererId,
            UpdateResourceInventoryRequest request) {
        PerishableResource resource = findResourceOwnedBy(id, offererId);
        validateInventoryRequest(request);

        if (request.stockQuantity() != null) {
            resource.setStockQuantity(request.stockQuantity());
        }
        if (request.expirationDate() != null) {
            resource.setExpirationDate(request.expirationDate());
        }

        resourceValidator.validateOperation(resource, BusinessOperation.UPDATE);
        boolean expirationDateInPast = checkExpirationWarning(resource);

        PerishableResource saved = resourceRepository.saveAndFlush(resource);
        announceIfDepleted(saved);

        return new ResourceInventoryUpdateDTO(resourceMapper.toDto(saved), expirationDateInPast);
    }

    /**
     * Finds a resource entity owned by the authenticated offerer.
     *
     * @param id              the resource id
     * @param offererId the authenticated offerer id
     * @return the resource entity
     * @throws EntityNotFoundException if the resource does not exist or belongs to another offerer
     */
    public PerishableResource findResourceOwnedBy(Long id, Long offererId) {
        PerishableResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(PerishableResource.class, id));

        if (!resource.getOfferer().getId().equals(offererId)) {
            throw new EntityNotFoundException(PerishableResource.class, id);
        }
        return resource;
    }

    /**
     * Deducts stock quantity for a resource owned by the authenticated offerer.
     *
     * @param id              the resource id
     * @param offererId the authenticated offerer id
     * @param quantity        the quantity to deduct
     * @return the updated resource entity
     * @throws EntityNotFoundException        if the resource does not exist or belongs to another offerer
     * @throws BusinessRuleViolationException if stock quantity is insufficient
     */
    public PerishableResource deductStock(Long id, Long offererId, int quantity) {
        PerishableResource resource = findResourceOwnedBy(id, offererId);

        if (resource.getStockQuantity() < quantity) {
            throw new BusinessRuleViolationException("Insufficient stock for resource id " + id);
        }

        resource.setStockQuantity(resource.getStockQuantity() - quantity);
        PerishableResource saved = resourceRepository.save(resource);
        announceIfDepleted(saved);

        return saved;
    }

    /**
     * Applies a discount to a resource owned by the authenticated offerer (UC07).
     *
     * <p>The percentage is always applied to the resource's <em>original</em>
     * price, never to the price already in effect, so "30% off" always means
     * 30% off the list price and two discounts in a row never compound.</p>
     *
     * @param id              the resource id
     * @param offererId the authenticated offerer id
     * @param percentage      the discount percentage to apply
     * @return the updated resource entity
     * @throws EntityNotFoundException if the resource does not exist or belongs to another offerer
     */
    public PerishableResource applyDiscount(Long id, Long offererId, Percentage percentage) {
        PerishableResource resource = findResourceOwnedBy(id, offererId);

        resource.setCurrentPrice(resource.getOriginalPrice().applyDiscount(percentage));
        return resourceRepository.save(resource);
    }

    /**
     * Announces that the resource ran out of stock, so whoever cares can react
     * (UC08 closes its open offers). Publishing instead of calling keeps this
     * service unaware that offers exist.
     *
     * @param resource the resource just saved
     */
    private void announceIfDepleted(PerishableResource resource) {
        if (resource.getStockQuantity() <= 0) {
            eventPublisher.publish(new ResourceStockDepletedEvent(
                    resource.getId(), resource.getOfferer().getId()));
        }
    }

    private void validateInventoryRequest(UpdateResourceInventoryRequest request) {
        if (request == null || (request.stockQuantity() == null && request.expirationDate() == null)) {
            throw new BusinessRuleViolationException(
                    "at least one inventory field must be informed");
        }
        if (request.stockQuantity() != null && request.stockQuantity() < 0) {
            throw new BusinessRuleViolationException("stockQuantity must not be negative");
        }
    }

    private boolean checkExpirationWarning(PerishableResource resource) {
        return resource.getExpirationDate() != null
                && resource.getExpirationDate().isBefore(timeProvider.now().toLocalDate());
    }
}
