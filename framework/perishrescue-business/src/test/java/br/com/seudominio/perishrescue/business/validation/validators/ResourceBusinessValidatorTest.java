package br.com.seudominio.perishrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class ResourceBusinessValidatorTest {

    private final Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

    private PerishableResource validResource() {
        return TestResource.builder()
                .name("Produto Padrão")
                .category("ALIMENTO")
                .originalPrice(Money.of(new BigDecimal("10.00")))
                .photoUrl("https://exemplo.com/foto.jpg")
                .build();
    }

    @Test
    void passesForValidResource() {
        ResourceBusinessValidator validator = new ResourceBusinessValidator(beanValidator);

        validator.validateOperation(validResource(), BusinessOperation.CREATE);
    }

    @Test
    void rejectsNullOriginalPrice() {
        ResourceBusinessValidator validator = new ResourceBusinessValidator(beanValidator);

        PerishableResource resource = validResource();
        resource.setOriginalPrice(null);

        assertThatThrownBy(() -> validator.validateOperation(resource, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("ORIGINAL_PRICE_NOT_POSITIVE")));
    }

    @Test
    void rejectsZeroOriginalPrice() {
        ResourceBusinessValidator validator = new ResourceBusinessValidator(beanValidator);

        PerishableResource resource = validResource();
        // O valor zero não costuma disparar a IllegalArgumentException do Value Object,
        // mas deve cair na regra de isPositive() do seu BusinessValidator.
        resource.setOriginalPrice(Money.of(BigDecimal.ZERO));

        assertThatThrownBy(() -> validator.validateOperation(resource, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("ORIGINAL_PRICE_NOT_POSITIVE")));
    }

    @Test
    void rejectsBlankName() {
        ResourceBusinessValidator validator = new ResourceBusinessValidator(beanValidator);

        PerishableResource resource = validResource();
        resource.setName(" ");

        assertThatThrownBy(() -> validator.validateOperation(resource, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CONSTRAINT_VIOLATION")));
    }

    @Test
    void rejectsBlankCategory() {
        ResourceBusinessValidator validator = new ResourceBusinessValidator(beanValidator);

        PerishableResource resource = validResource();
        resource.setCategory(" ");

        assertThatThrownBy(() -> validator.validateOperation(resource, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CONSTRAINT_VIOLATION")));
    }
}