package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.validation.validators.ProductBusinessValidator;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.ProductDTO;
import br.com.seudominio.foodrescue.domain.dtos.ProductInventoryUpdateDTO;
import br.com.seudominio.foodrescue.domain.dtos.RegisterProductDTO;
import br.com.seudominio.foodrescue.domain.dtos.UpdateProductInventoryRequest;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.ProductMapper;
import br.com.seudominio.foodrescue.persistence.repositories.EstablishmentRepository;
import br.com.seudominio.foodrescue.persistence.repositories.ProductRepository;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class ProductServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 12, 0);

    private ProductRepository productRepository;
    private EstablishmentRepository establishmentRepository;
    private TimeProvider timeProvider;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        establishmentRepository = mock(EstablishmentRepository.class);
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        ProductMapper productMapper = new ProductMapper();

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any(Product.class))).thenReturn(Collections.emptySet());
        ProductBusinessValidator productValidator = new ProductBusinessValidator(beanValidator);

        MessageUtils messageUtils = mock(MessageUtils.class);

        productService = new ProductService(
                productRepository, establishmentRepository, productMapper,
                beanValidator, messageUtils, productValidator, timeProvider);
    }

    private RegisterProductDTO registerDto(BigDecimal originalPrice) {
        return new RegisterProductDTO("Coxinha", "salgado", originalPrice, null);
    }

    private Establishment establishment(Long id, String name) {
        Establishment establishment = new Establishment();
        establishment.setId(id);
        establishment.setName(name);
        return establishment;
    }

    private Product product(Long id, Establishment owner) {
        Product product = new Product();
        product.setId(id);
        product.setName("Coxinha");
        product.setCategory("salgado");
        product.setOriginalPrice(br.com.seudominio.foodrescue.core.money.Money.of(new BigDecimal("12.50")));
        product.setCurrentPrice(br.com.seudominio.foodrescue.core.money.Money.of(new BigDecimal("12.50")));
        product.setEstablishment(owner);
        return product;
    }


    @Test
    void registerProductWithValidDataSavesWithDefaultsAndReturnsDto() {
        RegisterProductDTO dto = registerDto(new BigDecimal("12.50"));
        Establishment establishment = establishment(1L, "Padaria da Esquina");

        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(10L);
            return product;
        });

        ProductDTO result = productService.registerProduct(dto, 1L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.name()).isEqualTo("Coxinha");
        assertThat(result.originalPrice()).isEqualByComparingTo("12.50");
        assertThat(result.currentPrice()).isEqualByComparingTo("12.50");
        assertThat(result.establishmentId()).isEqualTo(1L);
    }

    @Test
    void registerProductWhenEstablishmentDoesNotExistThrowsEntityNotFoundException() {
        RegisterProductDTO dto = registerDto(new BigDecimal("12.50"));
        when(establishmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.registerProduct(dto, 99L))
                .isInstanceOf(EntityNotFoundException.class);
    }


    @Test
    void registerProductWithZeroOriginalPriceThrowsValidationException() {
        RegisterProductDTO dto = registerDto(BigDecimal.ZERO);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment(1L, "Padaria da Esquina")));

        assertThatThrownBy(() -> productService.registerProduct(dto, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("originalPrice must be greater than zero");
    }

    @Test
    void registerProductWithNegativeOriginalPriceThrowsException() {
        RegisterProductDTO dto = registerDto(new BigDecimal("-5.00"));
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment(1L, "Padaria da Esquina")));

        assertThatThrownBy(() -> productService.registerProduct(dto, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid money amount");
    }


    @Test
    void findAllForEstablishmentReturnsOnlyThatEstablishmentsProducts() {
        Establishment establishment = establishment(1L, "Padaria da Esquina");
        Product product = product(10L, establishment);

        when(establishmentRepository.existsById(1L)).thenReturn(true);
        when(productRepository.findAllByEstablishmentIdAndActiveTrue(1L)).thenReturn(List.of(product));

        List<ProductDTO> result = productService.findAllForEstablishment(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);
        assertThat(result.get(0).establishmentId()).isEqualTo(1L);
    }

    @Test
    void findAllForEstablishmentWhenEstablishmentDoesNotExistThrowsEntityNotFoundException() {
        when(establishmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findAllForEstablishment(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }


    @Test
    void findByIdForEstablishmentReturnsProductWhenOwnedByRequester() {
        Establishment owner = establishment(1L, "Padaria da Esquina");
        Product product = product(10L, owner);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductDTO result = productService.findByIdForEstablishment(10L, 1L);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.establishmentId()).isEqualTo(1L);
    }

    @Test
    void findByIdForEstablishmentThrowsEntityNotFoundWhenOwnedByAnotherEstablishment() {
        Establishment owner = establishment(1L, "Padaria da Esquina");
        Product product = product(10L, owner);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.findByIdForEstablishment(10L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findByIdForEstablishmentThrowsEntityNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findByIdForEstablishment(404L, 1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateInventoryChangesStockAndExpirationDateAndReturnsUpdatedProduct() {
        Product product = product(10L, establishment(1L, "Padaria da Esquina"));
        LocalDate newExpirationDate = NOW.toLocalDate().plusDays(5);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        mockInventorySave();

        ProductInventoryUpdateDTO result = productService.updateInventory(
                10L, 1L, new UpdateProductInventoryRequest(8, newExpirationDate));

        assertThat(result.product().stockQuantity()).isEqualTo(8);
        assertThat(result.product().expirationDate()).isEqualTo(newExpirationDate);
        assertThat(result.product().modificationDate()).isEqualTo(NOW);
        assertThat(result.expirationDateInPast()).isFalse();
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void updateInventoryChangesOnlyTheInformedField() {
        Product product = product(10L, establishment(1L, "Padaria da Esquina"));
        LocalDate originalExpirationDate = NOW.toLocalDate().plusDays(3);
        product.setStockQuantity(2);
        product.setExpirationDate(originalExpirationDate);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        mockInventorySave();

        ProductInventoryUpdateDTO result = productService.updateInventory(
                10L, 1L, new UpdateProductInventoryRequest(7, null));

        assertThat(result.product().stockQuantity()).isEqualTo(7);
        assertThat(result.product().expirationDate()).isEqualTo(originalExpirationDate);
    }

    @Test
    void updateInventoryAcceptsZeroStock() {
        Product product = product(10L, establishment(1L, "Padaria da Esquina"));
        product.setStockQuantity(3);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        mockInventorySave();

        ProductInventoryUpdateDTO result = productService.updateInventory(
                10L, 1L, new UpdateProductInventoryRequest(0, null));

        assertThat(result.product().stockQuantity()).isZero();
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void updateInventoryWithNegativeStockThrowsBusinessRuleViolationException() {
        Product product = product(10L, establishment(1L, "Padaria da Esquina"));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateInventory(
                10L, 1L, new UpdateProductInventoryRequest(-1, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("stockQuantity must not be negative");

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateInventoryWithPastExpirationDateSavesAndReturnsWarning() {
        Product product = product(10L, establishment(1L, "Padaria da Esquina"));
        LocalDate pastDate = NOW.toLocalDate().minusDays(1);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        mockInventorySave();

        ProductInventoryUpdateDTO result = productService.updateInventory(
                10L, 1L, new UpdateProductInventoryRequest(null, pastDate));

        assertThat(result.product().expirationDate()).isEqualTo(pastDate);
        assertThat(result.expirationDateInPast()).isTrue();
        verify(productRepository).saveAndFlush(product);
    }

    @Test
    void updateInventoryWithExpirationDateTodayDoesNotReturnWarning() {
        Product product = product(10L, establishment(1L, "Padaria da Esquina"));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        mockInventorySave();

        ProductInventoryUpdateDTO result = productService.updateInventory(
                10L, 1L, new UpdateProductInventoryRequest(null, NOW.toLocalDate()));

        assertThat(result.expirationDateInPast()).isFalse();
    }

    @Test
    void updateInventoryWithoutFieldsThrowsBusinessRuleViolationException() {
        Product product = product(10L, establishment(1L, "Padaria da Esquina"));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateInventory(
                10L, 1L, new UpdateProductInventoryRequest(null, null)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessage("at least one inventory field must be informed");

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateInventoryThrowsEntityNotFoundWhenProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateInventory(
                404L, 1L, new UpdateProductInventoryRequest(2, null)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateInventoryThrowsEntityNotFoundWhenProductBelongsToAnotherEstablishment() {
        Product product = product(10L, establishment(2L, "Outro Estabelecimento"));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.updateInventory(
                10L, 1L, new UpdateProductInventoryRequest(2, null)))
                .isInstanceOf(EntityNotFoundException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }

    private void mockInventorySave() {
        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setModificationDate(NOW);
            return saved;
        });
    }
}
