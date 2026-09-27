package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.validation.validators.SaleBusinessValidator;
import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.RegisterSaleDTO;
import br.com.seudominio.foodrescue.domain.dtos.SaleDTO;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.SaleMapper;
import br.com.seudominio.foodrescue.persistence.repositories.SaleRepository;
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
    private static final Long ESTABLISHMENT_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    private SaleRepository saleRepository;
    private ProductService productService;
    private TimeProvider timeProvider;
    private SaleService saleService;

    @BeforeEach
    void setUp() {
        saleRepository = mock(SaleRepository.class);
        productService = mock(ProductService.class);
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        SaleMapper saleMapper = new SaleMapper();

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any(Sale.class))).thenReturn(Collections.emptySet());
        SaleBusinessValidator saleValidator = new SaleBusinessValidator(beanValidator, timeProvider);

        MessageUtils messageUtils = mock(MessageUtils.class);

        saleService = new SaleService(
                saleRepository,
                productService,
                saleMapper,
                beanValidator,
                messageUtils,
                saleValidator,
                timeProvider
        );
    }

    private Establishment createEstablishment(Long id) {
        Establishment establishment = new Establishment();
        establishment.setId(id);
        establishment.setName("Padaria Boa Massa");
        return establishment;
    }

    private Product createProduct(Long id, Long establishmentId, int stock, BigDecimal currentPrice) {
        Product product = new Product();
        product.setId(id);
        product.setName("Pão Artesanal");
        product.setCategory("PADARIA");
        product.setOriginalPrice(Money.of(new BigDecimal("10.00")));
        product.setCurrentPrice(Money.of(currentPrice));
        product.setStockQuantity(stock);
        product.setEstablishment(createEstablishment(establishmentId));
        return product;
    }

    @Test
    void registerSaleWithExplicitPriceAndDateDeductsStockViaProductServiceAndSavesSale() {
        Product product = createProduct(PRODUCT_ID, ESTABLISHMENT_ID, 7, new BigDecimal("8.00"));
        when(productService.deductStock(PRODUCT_ID, ESTABLISHMENT_ID, 3)).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(100L);
            return s;
        });

        LocalDateTime customSoldAt = NOW.minusMinutes(30);
        RegisterSaleDTO dto = new RegisterSaleDTO(PRODUCT_ID, 3, new BigDecimal("7.50"), customSoldAt);

        SaleDTO result = saleService.registerSale(dto, ESTABLISHMENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.quantity()).isEqualTo(3);
        assertThat(result.unitPrice()).isEqualByComparingTo("7.50");
        assertThat(result.totalPrice()).isEqualByComparingTo("22.50");
        assertThat(result.soldAt()).isEqualTo(customSoldAt);

        verify(productService).deductStock(PRODUCT_ID, ESTABLISHMENT_ID, 3);
        verify(saleRepository).save(any(Sale.class));
    }

    @Test
    void registerSaleWithDefaultPriceAndDateUsesProductCurrentPriceAndTimeProvider() {
        Product product = createProduct(PRODUCT_ID, ESTABLISHMENT_ID, 3, new BigDecimal("6.00"));
        when(productService.deductStock(PRODUCT_ID, ESTABLISHMENT_ID, 2)).thenReturn(product);
        when(saleRepository.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale s = invocation.getArgument(0);
            s.setId(101L);
            return s;
        });

        RegisterSaleDTO dto = new RegisterSaleDTO(PRODUCT_ID, 2, null, null);

        SaleDTO result = saleService.registerSale(dto, ESTABLISHMENT_ID);

        assertThat(result.id()).isEqualTo(101L);
        assertThat(result.unitPrice()).isEqualByComparingTo("6.00");
        assertThat(result.totalPrice()).isEqualByComparingTo("12.00");
        assertThat(result.soldAt()).isEqualTo(NOW);

        verify(productService).deductStock(PRODUCT_ID, ESTABLISHMENT_ID, 2);
    }

    @Test
    void registerSaleThrowsEntityNotFoundExceptionWhenProductDoesNotExist() {
        when(productService.deductStock(999L, ESTABLISHMENT_ID, 1))
                .thenThrow(new EntityNotFoundException(Product.class, 999L));

        RegisterSaleDTO dto = new RegisterSaleDTO(999L, 1, null, null);

        assertThatThrownBy(() -> saleService.registerSale(dto, ESTABLISHMENT_ID))
                .isInstanceOf(EntityNotFoundException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    void registerSaleThrowsBusinessRuleViolationExceptionWhenStockIsInsufficient() {
        when(productService.deductStock(PRODUCT_ID, ESTABLISHMENT_ID, 5))
                .thenThrow(new BusinessRuleViolationException("Insufficient stock for product id " + PRODUCT_ID));

        RegisterSaleDTO dto = new RegisterSaleDTO(PRODUCT_ID, 5, null, null);

        assertThatThrownBy(() -> saleService.registerSale(dto, ESTABLISHMENT_ID))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("Insufficient stock");

        verify(saleRepository, never()).save(any());
    }

    @Test
    void registerSaleThrowsValidationExceptionWhenSoldAtIsInTheFuture() {
        Product product = createProduct(PRODUCT_ID, ESTABLISHMENT_ID, 10, new BigDecimal("5.00"));
        when(productService.deductStock(PRODUCT_ID, ESTABLISHMENT_ID, 1)).thenReturn(product);

        RegisterSaleDTO dto = new RegisterSaleDTO(PRODUCT_ID, 1, null, NOW.plusDays(1));

        assertThatThrownBy(() -> saleService.registerSale(dto, ESTABLISHMENT_ID))
                .isInstanceOf(ValidationException.class);

        verify(saleRepository, never()).save(any());
    }

    @Test
    void findSalesByProductAndPeriodReturnsSalesList() {
        Product product = createProduct(PRODUCT_ID, ESTABLISHMENT_ID, 10, new BigDecimal("10.00"));
        when(productService.findProductOwnedBy(PRODUCT_ID, ESTABLISHMENT_ID)).thenReturn(product);

        LocalDateTime start = NOW.minusDays(7);
        LocalDateTime end = NOW;

        Sale sale = Sale.builder()
                .id(200L)
                .product(product)
                .quantity(4)
                .unitPrice(Money.of(new BigDecimal("10.00")))
                .soldAt(NOW.minusDays(1))
                .build();

        when(saleRepository.findByProductAndPeriod(PRODUCT_ID, start, end))
                .thenReturn(List.of(sale));

        List<SaleDTO> result = saleService.findSalesByProductAndPeriod(PRODUCT_ID, ESTABLISHMENT_ID, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(200L);
        assertThat(result.get(0).productId()).isEqualTo(PRODUCT_ID);
        assertThat(result.get(0).totalPrice()).isEqualByComparingTo("40.00");

        verify(productService).findProductOwnedBy(PRODUCT_ID, ESTABLISHMENT_ID);
        verify(saleRepository).findByProductAndPeriod(PRODUCT_ID, start, end);
    }

    @Test
    void findSalesByProductAndPeriodThrowsWhenStartDateIsAfterEndDate() {
        Product product = createProduct(PRODUCT_ID, ESTABLISHMENT_ID, 10, new BigDecimal("10.00"));
        when(productService.findProductOwnedBy(PRODUCT_ID, ESTABLISHMENT_ID)).thenReturn(product);

        LocalDateTime start = NOW.plusDays(1);
        LocalDateTime end = NOW;

        assertThatThrownBy(() -> saleService.findSalesByProductAndPeriod(PRODUCT_ID, ESTABLISHMENT_ID, start, end))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("startDate must not be after endDate");
    }

    @Test
    void findByIdForEstablishmentReturnsSaleDtoWhenOwned() {
        Product product = createProduct(PRODUCT_ID, ESTABLISHMENT_ID, 10, new BigDecimal("10.00"));
        Sale sale = Sale.builder()
                .id(300L)
                .product(product)
                .quantity(2)
                .unitPrice(Money.of(new BigDecimal("10.00")))
                .soldAt(NOW.minusHours(2))
                .build();

        when(saleRepository.findById(300L)).thenReturn(Optional.of(sale));

        SaleDTO dto = saleService.findByIdForEstablishment(300L, ESTABLISHMENT_ID);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(300L);
        assertThat(dto.productId()).isEqualTo(PRODUCT_ID);
    }

    @Test
    void findByIdForEstablishmentThrowsEntityNotFoundWhenSaleBelongsToAnotherEstablishment() {
        Product otherProduct = createProduct(PRODUCT_ID, 99L, 10, new BigDecimal("10.00"));
        Sale sale = Sale.builder()
                .id(300L)
                .product(otherProduct)
                .quantity(2)
                .unitPrice(Money.of(new BigDecimal("10.00")))
                .soldAt(NOW.minusHours(2))
                .build();

        when(saleRepository.findById(300L)).thenReturn(Optional.of(sale));

        assertThatThrownBy(() -> saleService.findByIdForEstablishment(300L, ESTABLISHMENT_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByIdForEstablishmentThrowsEntityNotFoundWhenSaleDoesNotExist() {
        when(saleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.findByIdForEstablishment(999L, ESTABLISHMENT_ID))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
