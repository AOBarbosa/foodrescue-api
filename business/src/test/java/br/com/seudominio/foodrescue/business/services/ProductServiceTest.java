package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.validation.validators.ProductBusinessValidator;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.ProductDTO;
import br.com.seudominio.foodrescue.domain.dtos.RegisterProductDTO;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.ProductMapper;
import br.com.seudominio.foodrescue.persistence.repositories.EstablishmentRepository;
import br.com.seudominio.foodrescue.persistence.repositories.ProductRepository;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

class ProductServiceTest {

    private ProductRepository productRepository;
    private EstablishmentRepository establishmentRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        establishmentRepository = mock(EstablishmentRepository.class);

        ProductMapper productMapper = new ProductMapper();

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any(Product.class))).thenReturn(Collections.emptySet());
        ProductBusinessValidator productValidator = new ProductBusinessValidator(beanValidator);

        MessageUtils messageUtils = mock(MessageUtils.class);

        productService = new ProductService(
                productRepository, establishmentRepository, productMapper,
                beanValidator, messageUtils, productValidator);
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must not be negative");
    }


    @Test
    void findAllForEstablishmentReturnsOnlyThatEstablishmentsProducts() {
        Establishment establishment = establishment(1L, "Padaria da Esquina");
        Product product = product(10L, establishment);

        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));
        when(productRepository.findAllByEstablishmentId(1L)).thenReturn(List.of(product));

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
}