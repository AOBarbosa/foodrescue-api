package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.validation.validators.SaleBusinessValidator;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.TimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.dtos.RegisterSaleDTO;
import br.com.seudominio.perishrescue.domain.dtos.SaleDTO;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.entities.Sale;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.SaleMapper;
import br.com.seudominio.perishrescue.persistence.repositories.SaleRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class SaleServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 12, 0);
    private static final Long OFFERER_ID = 1L;
    private static final Long RESOURCE_ID = 10L;

    private SaleRepository saleRepository;
    private ResourceService resourceService;
    private TimeProvider timeProvider;
    private SaleService saleService;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        resourceService = mock(ResourceService.class);
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        SaleMapper saleMapper = new SaleMapper();

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any(Sale.class))).thenReturn(Collections.emptySet());
        SaleBusinessValidator saleValidator = new SaleBusinessValidator(beanValidator, timeProvider);

        MessageUtils messageUtils = mock(MessageUtils.class);

        saleService = new SaleService(
                saleRepository,
                resourceService,
                saleMapper,
                beanValidator,
                messageUtils,
                saleValidator,
                timeProvider
        );
    }

    private Offerer createOfferer(Long id) {
        Offerer offerer = new Offerer();
        offerer.setId(id);
        offerer.setName("Padaria Boa Massa");
        return offerer;
    }

    private PerishableResource createResource(Long id, Long offererId, int stock, BigDecimal currentPrice) {
        PerishableResource resource = new TestResource();
        resource.setId(id);
        resource.setName("Pão Artesanal");
        resource.setCategory("PADARIA");
        resource.setOriginalPrice(Money.of(new BigDecimal("10.00")));
        resource.setCurrentPrice(Money.of(currentPrice));
        resource.setStockQuantity(stock);
        resource.setOfferer(createOfferer(offererId));
        return resource;
    }

    @Test
    void registerSaleWithExplicitPriceAndDateDeductsStockViaResourceServiceAndSavesSale() {
        PerishableResource resource = createResource(RESOURCE_ID, OFFERER_ID, 7, new BigDecimal("8.00"));
        when(resourceService.deductStock(RESOURCE_ID, OFFERER_ID, 3)).thenReturn(resource);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        LocalDateTime customSoldAt = NOW.minusMinutes(30);
        RegisterSaleDTO dto = new RegisterSaleDTO(RESOURCE_ID, 3, new BigDecimal("7.50"), customSoldAt);

        SaleDTO result = saleService.registerSale(dto, OFFERER_ID);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.resourceId()).isEqualTo(RESOURCE_ID);
        assertThat(result.quantity()).isEqualTo(3);
        assertThat(result.unitPrice()).isEqualByComparingTo("7.50");
        assertThat(result.totalPrice()).isEqualByComparingTo("22.50");
        assertThat(result.soldAt()).isEqualTo(customSoldAt);

        verify(resourceService).deductStock(RESOURCE_ID, OFFERER_ID, 3);
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    void registerSaleWithDefaultPriceAndDateUsesResourceCurrentPriceAndTimeProvider() {
        PerishableResource resource = createResource(RESOURCE_ID, OFFERER_ID, 3, new BigDecimal("6.00"));
        when(resourceService.deductStock(RESOURCE_ID, OFFERER_ID, 2)).thenReturn(resource);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(101L);
            return s;
        });

        RegisterSaleDTO dto = new RegisterSaleDTO(RESOURCE_ID, 2, null, null);

        SaleDTO result = saleService.registerSale(dto, OFFERER_ID);

        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.unitPrice()).isEqualByComparingTo("6.00");
        assertThat(result.totalPrice()).isEqualByComparingTo("12.00");
        assertThat(result.soldAt()).isEqualTo(NOW);

        verify(resourceService).deductStock(RESOURCE_ID, OFFERER_ID, 2);
    }

    @Test
    void registerSaleThrowsEntityNotFoundExceptionWhenResourceDoesNotExist() {
        when(resourceService.deductStock(999L, OFFERER_ID, 1))
                .thenThrow(new EntityNotFoundException(PerishableResource.class, 999L));

        RegisterSaleDTO dto = new RegisterSaleDTO(999L, 1, null, null);

        assertThatThrownBy(() -> saleService.registerSale(dto, OFFERER_ID))
                .isInstanceOf(EntityNotFoundException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    void registerSaleThrowsBusinessRuleViolationExceptionWhenStockIsInsufficient() {
        when(resourceService.deductStock(RESOURCE_ID, OFFERER_ID, 5))
                .thenThrow(new BusinessRuleViolationException("Insufficient stock for resource id " + RESOURCE_ID));

        RegisterSaleDTO dto = new RegisterSaleDTO(RESOURCE_ID, 5, null, null);

        assertThatThrownBy(() -> saleService.registerSale(dto, OFFERER_ID))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Insufficient stock");

        verify(saleRepository, never()).save(any());
    }

    @Test
    void registerSaleThrowsValidationExceptionWhenSoldAtIsInTheFuture() {
        PerishableResource resource = createResource(RESOURCE_ID, OFFERER_ID, 10, new BigDecimal("5.00"));
        when(resourceService.deductStock(RESOURCE_ID, OFFERER_ID, 1)).thenReturn(resource);

        RegisterSaleDTO dto = new RegisterSaleDTO(RESOURCE_ID, 1, null, NOW.plusDays(1));

        assertThatThrownBy(() -> saleService.registerSale(dto, OFFERER_ID))
                .isInstanceOf(ValidationException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    void findSalesByResourceAndPeriodReturnsSalesList() {
        PerishableResource resource = createResource(RESOURCE_ID, OFFERER_ID, 10, new BigDecimal("10.00"));
        when(resourceService.findResourceOwnedBy(RESOURCE_ID, OFFERER_ID)).thenReturn(resource);

        LocalDateTime start = NOW.minusDays(7);
        LocalDateTime end = NOW;

        Sale sale = Sale.builder()
                .id(200L)
                .resource(resource)
                .quantity(4)
                .unitPrice(Money.of(new BigDecimal("10.00")))
                .soldAt(NOW.minusDays(1))
                .build();

        when(saleRepository.findByResourceAndPeriod(RESOURCE_ID, start, end))
                .thenReturn(List.of(sale));

        List<SaleDTO> result = saleService.findSalesByResourceAndPeriod(RESOURCE_ID, OFFERER_ID, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(200L);
        assertThat(result.get(0).resourceId()).isEqualTo(RESOURCE_ID);
        assertThat(result.get(0).totalPrice()).isEqualByComparingTo("40.00");

        verify(resourceService).findResourceOwnedBy(RESOURCE_ID, OFFERER_ID);
        verify(saleRepository).findByResourceAndPeriod(RESOURCE_ID, start, end);
    }

    @Test
    void findSalesByResourceAndPeriodThrowsWhenStartDateIsAfterEndDate() {
        PerishableResource resource = createResource(RESOURCE_ID, OFFERER_ID, 10, new BigDecimal("10.00"));
        when(resourceService.findResourceOwnedBy(RESOURCE_ID, OFFERER_ID)).thenReturn(resource);

        LocalDateTime start = NOW.plusDays(1);
        LocalDateTime end = NOW;

        assertThatThrownBy(() -> saleService.findSalesByResourceAndPeriod(RESOURCE_ID, OFFERER_ID, start, end))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("startDate must not be after endDate");
    }

    @Test
    void findByIdForOffererReturnsSaleDtoWhenOwned() {
        PerishableResource resource = createResource(RESOURCE_ID, OFFERER_ID, 10, new BigDecimal("10.00"));
        Sale sale = Sale.builder()
                .id(300L)
                .resource(resource)
                .quantity(2)
                .unitPrice(Money.of(new BigDecimal("10.00")))
                .soldAt(NOW.minusHours(2))
                .build();

        when(saleRepository.findById(300L)).thenReturn(Optional.of(sale));

        SaleDTO dto = saleService.findByIdForOfferer(300L, OFFERER_ID);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(300L);
        assertThat(dto.resourceId()).isEqualTo(RESOURCE_ID);
    }

    @Test
    void findByIdForOffererThrowsEntityNotFoundWhenSaleBelongsToAnotherOfferer() {
        PerishableResource otherResource = createResource(RESOURCE_ID, 99L, 10, new BigDecimal("10.00"));
        Sale sale = Sale.builder()
                .id(300L)
                .resource(otherResource)
                .quantity(2)
                .unitPrice(Money.of(new BigDecimal("10.00")))
                .soldAt(NOW.minusHours(2))
                .build();

        when(saleRepository.findById(300L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> saleService.findByIdForOfferer(300L, OFFERER_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByIdForOffererThrowsEntityNotFoundWhenSaleDoesNotExist() {
        when(saleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.findByIdForOfferer(999L, OFFERER_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
