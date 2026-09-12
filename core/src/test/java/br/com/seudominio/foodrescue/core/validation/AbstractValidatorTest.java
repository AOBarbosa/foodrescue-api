package br.com.seudominio.foodrescue.core.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;

import org.junit.jupiter.api.Test;

class AbstractValidatorTest {

    private static final class NameNotBlankValidator extends AbstractValidator<String> {
        @Override
        protected void doValidate(String entity) {
            if (entity == null || entity.isBlank()) {
                addError("name is required", "name", entity, "NAME_REQUIRED");
            }
        }
    }

    @Test
    void passesWhenNoErrorsAccumulated() {
        new NameNotBlankValidator().validate("Maria");
    }

    @Test
    void throwsValidationExceptionCarryingAccumulatedErrors() {
        NameNotBlankValidator validator = new NameNotBlankValidator();

        assertThatThrownBy(() -> validator.validate(" "))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> {
                    ValidationException validationException = (ValidationException) ex;
                    assertThat(validationException.getErrors()).hasSize(1);
                    assertThat(validationException.getErrors().get(0).code()).isEqualTo("NAME_REQUIRED");
                });
    }

    @Test
    void clearsErrorsBetweenValidateCalls() {
        NameNotBlankValidator validator = new NameNotBlankValidator();

        assertThatThrownBy(() -> validator.validate(" ")).isInstanceOf(ValidationException.class);

        validator.validate("Maria");
    }
}
