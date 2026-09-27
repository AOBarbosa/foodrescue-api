package br.com.seudominio.foodrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.entities.Sale;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

class SaleBusinessValidatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 26, 12, 0);

    private final Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();
    private TimeProvider timeProvider;
    private SaleBusinessValidator validator;

    @BeforeEach
    void setUp() {
        timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);
        validator = new SaleBusinessValidator(beanValidator, timeProvider);
    }

    private Sale validSale() {
        Product product = new Product();
        product.setId(1L);

        return Sale.builder()
                .product(product)
                .quantity(2)
                .unitPrice(Money.of(new BigDecimal("15.00")))
                .soldAt(NOW.minusHours(1))
                .build();
    }

    @Test
    void passesForValidSale() {
        validator.validateOperation(validSale(), BusinessOperation.CREATE);
    }

    @Test
    void rejectsNullEntity() {
        assertThatThrownBy(() -> validator.validateOperation(null, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("NULL_ENTITY")));
    }

    @Test
    void rejectsZeroOrNegativeQuantity() {
        Sale saleZero = validSale();
        saleZero.setQuantity(0);

        assertThatThrownBy(() -> validator.validateOperation(saleZero, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("QUANTITY_NOT_POSITIVE")));

        Sale saleNegative = validSale();
        saleNegative.setQuantity(-3);

        assertThatThrownBy(() -> validator.validateOperation(saleNegative, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("QUANTITY_NOT_POSITIVE")));
    }

    @Test
    void rejectsNullOrNonPositiveUnitPrice() {
        Sale saleNullPrice = validSale();
        saleNullPrice.setUnitPrice(null);

        assertThatThrownBy(() -> validator.validateOperation(saleNullPrice, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("UNIT_PRICE_NOT_POSITIVE")));

        Sale saleZeroPrice = validSale();
        saleZeroPrice.setUnitPrice(Money.of(BigDecimal.ZERO));

        assertThatThrownBy(() -> validator.validateOperation(saleZeroPrice, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("UNIT_PRICE_NOT_POSITIVE")));
    }

    @Test
    void rejectsNullOrFutureSoldAt() {
        Sale saleNullDate = validSale();
        saleNullDate.setSoldAt(null);

        assertThatThrownBy(() -> validator.validateOperation(saleNullDate, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("SOLD_AT_NULL")));

        Sale saleFuture = validSale();
        saleFuture.setSoldAt(NOW.plusDays(1));

        assertThatThrownBy(() -> validator.validateOperation(saleFuture, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("SOLD_AT_IN_FUTURE")));
    }
}
