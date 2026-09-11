package br.com.seudominio.foodrescue.business.validation.validators;

import br.com.seudominio.foodrescue.business.validation.BusinessValidator;
import br.com.seudominio.foodrescue.core.validation.AbstractValidator;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.persistence.repositories.ConsumerRepository;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Business validator for {@link Consumer} entity.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
@Component
public class ConsumerBusinessValidator extends AbstractValidator<Consumer> implements BusinessValidator<Consumer> {

    private final Validator validator;

    private final ConsumerRepository consumerRepository;

    /**
     * Constructor for {@code ConsumerBusinessValidator} with dependency injection.
     *
     * @param validator           the bean validator
     * @param consumerRepository  the consumer repository
     */
    public ConsumerBusinessValidator(Validator validator, ConsumerRepository consumerRepository) {
        this.validator = validator;
        this.consumerRepository = consumerRepository;
    }

    /**
     * Validates the given consumer entity.
     *
     * @param consumer the consumer entity to validate
     */
    @Override
    protected void doValidate(Consumer consumer) {
        validator.validate(consumer).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateEmailUnique(consumer);
    }

    /**
     * Validates the given consumer entity for the specified business operation.
     *
     * @param entity    the consumer entity to validate
     * @param operation the business operation to validate against
     */
    @Override
    public void validateOperation(Consumer entity, BusinessOperation operation) {
        errors.clear();
        doValidate(entity);

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed: " + getMessagesErrors(), errors);
        }
    }

    /**
     * Validates that the email of the consumer is unique.
     *
     * @param consumer the consumer entity to validate
     */
    private void validateEmailUnique(Consumer consumer) {
        if (consumer.getEmail() == null || consumer.getEmail().isBlank()) {
            return;
        }

        consumerRepository.findByEmail(consumer.getEmail())
                .filter(existing -> !Objects.equals(existing.getId(), consumer.getId()))
                .ifPresent(existing -> addError(
                        "email already registered",
                        "email",
                        consumer.getEmail(),
                        "EMAIL_ALREADY_EXISTS"));
    }
}
