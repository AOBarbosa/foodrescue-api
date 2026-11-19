package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.event.DomainEventPublisher;
import br.com.seudominio.perishrescue.business.event.ResourceStockDepletedEvent;
import br.com.seudominio.perishrescue.business.validation.validators.ResourceBusinessValidator;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
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
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class ResourceServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 12, 0);

    private ResourceRepository resourceRepository;
    private OffererRepository offererRepository;
    private TimeProvider timeProvider;
    private DomainEventPublisher eventPublisher;
    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        offererRepository = mock(OffererRepository.class);
        timeProvider = mock(TimeProvider.class);
        eventPublisher = mock(DomainEventPublisher.class);
        when(timeProvider.now()).thenReturn(NOW);

        ResourceMapper resourceMapper = new ResourceMapper();

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any(PerishableResource.class))).thenReturn(Collections.emptySet());
        ResourceBusinessValidator resourceValidator = new ResourceBusinessValidator(beanValidator);

        MessageUtils messageUtils = mock(MessageUtils.class);

        resourceService = new ResourceService(
                resourceRepository, offererRepository, resourceMapper,
                beanValidator, messageUtils, resourceValidator, timeProvider, eventPublisher);
    }

    private TestResource newResource(BigDecimal originalPrice) {
        return TestResource.builder()
                .name("Coxinha")
                .category("salgado")
                .originalPrice(Money.of(originalPrice))
                .build();
    }

    private Offerer offerer(Long id, String name) {
        Offerer offerer = new Offerer();
        offerer.setId(id);
        offerer.setName(name);
        return offerer;
    }

    private PerishableResource resource(Long id, Offerer owner) {
        PerishableResource resource = new TestResource();
        resource.setId(id);
        resource.setName("Coxinha");
        resource.setCategory("salgado");
        resource.setOriginalPrice(Money.of(new BigDecimal("12.50")));
        resource.setCurrentPrice(Money.of(new BigDecimal("12.50")));
        resource.setOfferer(owner);
        return resource;
    }

    private PerishableResource resource(Long id, Offerer owner, int stockQuantity) {
        PerishableResource resource = resource(id, owner);
        resource.setStockQuantity(stockQuantity);
        return resource;
    }


    @Test
    void registerAttachesTheOffererDefaultsTheCurrentPriceAndSaves() {
        TestResource newResource = newResource(new BigDecimal("12.50"));
        Offerer offerer = offerer(1L, "Padaria da Esquina");

        when(offererRepository.findById(1L)).thenReturn(Optional.of(offerer));
        when(resourceRepository.save(any(PerishableResource.class))).thenAnswer(invocation -> {
            PerishableResource resource = invocation.getArgument(0);
            resource.setId(10L);
            return resource;
        });

        TestResource result = resourceService.register(newResource, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Coxinha");
        assertThat(result.getOriginalPrice().amount()).isEqualByComparingTo("12.50");
        assertThat(result.getCurrentPrice().amount()).isEqualByComparingTo("12.50");
        assertThat(result.getOfferer().getId()).isEqualTo(1L);
    }

    @Test
    void registerResourceWhenOffererDoesNotExistThrowsEntityNotFoundException() {
        TestResource newResource = newResource(new BigDecimal("12.50"));
        when(offererRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.register(newResource, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }


    @Test
    void registerResourceWithZeroOriginalPriceThrowsValidationException() {
        TestResource newResource = newResource(BigDecimal.ZERO);
        when(offererRepository.findById(1L)).thenReturn(Optional.of(offerer(1L, "Padaria da Esquina")));

        assertThatThrownBy(() -> resourceService.register(newResource, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("originalPrice must be greater than zero");
    }

    @Test
    void registerResourceWithNegativeOriginalPriceThrowsException() {
                when(offererRepository.findById(1L)).thenReturn(Optional.of(offerer(1L, "Padaria da Esquina")));

        assertThatThrownBy(() -> resourceService.register(newResource(new BigDecimal("-5.00")), 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid money amount");
    }


    @Test
    void findAllForOffererReturnsOnlyThatOfferersResources() {
        Offerer offerer = offerer(1L, "Padaria da Esquina");
        PerishableResource resource = resource(10L, offerer);

        when(offererRepository.existsById(1L)).thenReturn(true);
        when(resourceRepository.findAllByOffererIdAndActiveTrue(1L)).thenReturn(List.of(resource));

        List<ResourceDTO> result = resourceService.findAllForOfferer(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).offererId()).isEqualTo(1L);
    }

    @Test
    void findAllForOffererWhenOffererDoesNotExistThrowsEntityNotFoundException() {
        when(offererRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.findAllForOfferer(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }


    @Test
    void findByIdForOffererReturnsResourceWhenOwnedByRequester() {
        Offerer owner = offerer(1L, "Padaria da Esquina");
        PerishableResource resource = resource(10L, owner);

        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        ResourceDTO result = resourceService.findByIdForOfferer(10L, 1L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.offererId()).isEqualTo(1L);
    }

    @Test
    void findByIdForOffererThrowsEntityNotFoundWhenOwnedByAnotherOfferer() {
        Offerer owner = offerer(1L, "Padaria da Esquina");
        PerishableResource resource = resource(10L, owner);

        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> resourceService.findByIdForOfferer(10L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByIdForOffererThrowsEntityNotFoundWhenResourceDoesNotExist() {
        when(resourceRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.findByIdForOfferer(404L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateInventoryChangesStockAndExpirationDateAndReturnsUpdatedResource() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria da Esquina"));
        LocalDate newExpirationDate = NOW.toLocalDate().plusDays(5);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        mockInventorySave();

        ResourceInventoryUpdateDTO result = resourceService.updateInventory(
                10L, 1L, new UpdateResourceInventoryRequest(8, newExpirationDate));

        assertThat(result.resource().stockQuantity()).isEqualTo(8);
        assertThat(result.resource().expirationDate()).isEqualTo(newExpirationDate);
        assertThat(result.resource().modificationDate()).isEqualTo(NOW);
        assertThat(result.expirationDateInPast()).isFalse();
        verify(resourceRepository).saveAndFlush(resource);
    }

    @Test
    void updateInventoryChangesOnlyTheInformedField() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria da Esquina"));
        LocalDate originalExpirationDate = NOW.toLocalDate().plusDays(3);
        resource.setStockQuantity(2);
        resource.setExpirationDate(originalExpirationDate);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        mockInventorySave();

        ResourceInventoryUpdateDTO result = resourceService.updateInventory(
                10L, 1L, new UpdateResourceInventoryRequest(7, null));

        assertThat(result.resource().stockQuantity()).isEqualTo(7);
        assertThat(result.resource().expirationDate()).isEqualTo(originalExpirationDate);
    }

    @Test
    void updateInventoryAcceptsZeroStock() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria da Esquina"));
        resource.setStockQuantity(3);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        mockInventorySave();

        ResourceInventoryUpdateDTO result = resourceService.updateInventory(
                10L, 1L, new UpdateResourceInventoryRequest(0, null));

        assertThat(result.resource().stockQuantity()).isZero();
        verify(resourceRepository).saveAndFlush(resource);
    }

    @Test
    void updateInventoryWithNegativeStockThrowsBusinessRuleViolationException() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria da Esquina"));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> resourceService.updateInventory(
                10L, 1L, new UpdateResourceInventoryRequest(-1, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("stockQuantity must not be negative");

        verify(resourceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateInventoryWithPastExpirationDateSavesAndReturnsWarning() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria da Esquina"));
        LocalDate pastDate = NOW.toLocalDate().minusDays(1);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        mockInventorySave();

        ResourceInventoryUpdateDTO result = resourceService.updateInventory(
                10L, 1L, new UpdateResourceInventoryRequest(null, pastDate));

        assertThat(result.resource().expirationDate()).isEqualTo(pastDate);
        assertThat(result.expirationDateInPast()).isTrue();
        verify(resourceRepository).saveAndFlush(resource);
    }

    @Test
    void updateInventoryWithExpirationDateTodayDoesNotReturnWarning() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria da Esquina"));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        mockInventorySave();

        ResourceInventoryUpdateDTO result = resourceService.updateInventory(
                10L, 1L, new UpdateResourceInventoryRequest(null, NOW.toLocalDate()));

        assertThat(result.expirationDateInPast()).isFalse();
    }

    @Test
    void updateInventoryWithoutFieldsThrowsBusinessRuleViolationException() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria da Esquina"));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> resourceService.updateInventory(
                10L, 1L, new UpdateResourceInventoryRequest(null, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("at least one inventory field must be informed");

        verify(resourceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateInventoryThrowsEntityNotFoundWhenResourceDoesNotExist() {
        when(resourceRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.updateInventory(
                404L, 1L, new UpdateResourceInventoryRequest(2, null)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(resourceRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateInventoryThrowsEntityNotFoundWhenResourceBelongsToAnotherOfferer() {
        PerishableResource resource = resource(10L, offerer(2L, "Outro Estabelecimento"));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> resourceService.updateInventory(
                10L, 1L, new UpdateResourceInventoryRequest(2, null)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(resourceRepository, never()).saveAndFlush(any());
    }

    private void mockInventorySave() {
        when(resourceRepository.saveAndFlush(any(PerishableResource.class))).thenAnswer(invocation -> {
            PerishableResource saved = invocation.getArgument(0);
            saved.setModificationDate(NOW);
            return saved;
        });
    }

    @Test
    void deductStockSuccessfullyDecrementsAndSavesResource() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria"));
        resource.setStockQuantity(15);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(PerishableResource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PerishableResource updated = resourceService.deductStock(10L, 1L, 5);

        assertThat(updated.getStockQuantity()).isEqualTo(10);
        verify(resourceRepository).save(resource);
    }

    @Test
    void deductStockThrowsBusinessRuleViolationExceptionWhenStockIsInsufficient() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria"));
        resource.setStockQuantity(3);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> resourceService.deductStock(10L, 1L, 5))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Insufficient stock for resource id 10");

        verify(resourceRepository, never()).save(any());
    }

    @Test
    void deductStockThrowsEntityNotFoundExceptionWhenResourceBelongsToAnotherOfferer() {
        PerishableResource resource = resource(10L, offerer(2L, "Outro Estabelecimento"));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        assertThatThrownBy(() -> resourceService.deductStock(10L, 1L, 2))
                .isInstanceOf(EntityNotFoundException.class);

        verify(resourceRepository, never()).save(any());
    }

    @Test
    void findResourceOwnedByReturnsResourceWhenOwned() {
        PerishableResource resource = resource(10L, offerer(1L, "Padaria"));
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));

        PerishableResource found = resourceService.findResourceOwnedBy(10L, 1L);

        assertThat(found).isSameAs(resource);
    }

    // ---------- UC08: anuncio de estoque esgotado ----------

    @Test
    void deductStockAnnouncesWhenTheStockReachesZero() {
        Offerer owner = offerer(1L, "Padaria");
        PerishableResource resource = resource(10L, owner, 4);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(PerishableResource.class))).thenAnswer(i -> i.getArgument(0));

        resourceService.deductStock(10L, 1L, 4);

        verify(eventPublisher).publish(new ResourceStockDepletedEvent(10L, 1L));
    }

    @Test
    void deductStockDoesNotAnnounceWhileStockRemains() {
        Offerer owner = offerer(1L, "Padaria");
        PerishableResource resource = resource(10L, owner, 10);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(PerishableResource.class))).thenAnswer(i -> i.getArgument(0));

        resourceService.deductStock(10L, 1L, 4);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateInventoryAnnouncesWhenTheStockIsSetToZero() {
        Offerer owner = offerer(1L, "Padaria");
        PerishableResource resource = resource(10L, owner, 10);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(resource));
        when(resourceRepository.saveAndFlush(any(PerishableResource.class))).thenAnswer(i -> i.getArgument(0));

        resourceService.updateInventory(10L, 1L, new UpdateResourceInventoryRequest(0, null));

        verify(eventPublisher).publish(new ResourceStockDepletedEvent(10L, 1L));
    }
}
