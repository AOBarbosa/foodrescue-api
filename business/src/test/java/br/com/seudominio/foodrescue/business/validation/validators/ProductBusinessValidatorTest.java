package br.com.seudominio.foodrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.entities.Product;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class ProductBusinessValidatorTest {

    private final Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

    private Product validProduct() {
        return Product.builder()
                .name("Produto Padrão")
                .category("ALIMENTO")
                .originalPrice(Money.of(new BigDecimal("10.00")))
                .photoUrl("https://exemplo.com/foto.jpg")
                .build();
    }

    @Test
    void passesForValidProduct() {
        ProductBusinessValidator validator = new ProductBusinessValidator(beanValidator);

        validator.validateOperation(validProduct(), BusinessOperation.CREATE);
    }

    @Test
    void rejectsNullOriginalPrice() {
        ProductBusinessValidator validator = new ProductBusinessValidator(beanValidator);

        Product product = validProduct();
        product.setOriginalPrice(null);

        assertThatThrownBy(() -> validator.validateOperation(product, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("ORIGINAL_PRICE_NOT_POSITIVE")));
    }

    @Test
    void rejectsZeroOriginalPrice() {
        ProductBusinessValidator validator = new ProductBusinessValidator(beanValidator);

        Product product = validProduct();
        // O valor zero não costuma disparar a IllegalArgumentException do Value Object,
        // mas deve cair na regra de isPositive() do seu BusinessValidator.
        product.setOriginalPrice(Money.of(BigDecimal.ZERO));

        assertThatThrownBy(() -> validator.validateOperation(product, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("ORIGINAL_PRICE_NOT_POSITIVE")));
    }

    @Test
    void rejectsBlankName() {
        ProductBusinessValidator validator = new ProductBusinessValidator(beanValidator);

        Product product = validProduct();
        product.setName(" ");

        assertThatThrownBy(() -> validator.validateOperation(product, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CONSTRAINT_VIOLATION")));
    }

    @Test
    void rejectsBlankCategory() {
        ProductBusinessValidator validator = new ProductBusinessValidator(beanValidator);

        Product product = validProduct();
        product.setCategory(" ");

        assertThatThrownBy(() -> validator.validateOperation(product, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CONSTRAINT_VIOLATION")));
    }
}