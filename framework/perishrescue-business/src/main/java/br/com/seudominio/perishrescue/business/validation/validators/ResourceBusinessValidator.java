package br.com.seudominio.perishrescue.business.validation.validators;

import br.com.seudominio.perishrescue.business.validation.BusinessValidator;
import br.com.seudominio.perishrescue.core.validation.AbstractValidator;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

/**
 * Business validator for {@link PerishableResource} entity.
 *
 * @since 1.0.0
 * @author Clovis Medeiros
 */
@Component
public class ResourceBusinessValidator extends AbstractValidator<PerishableResource> implements BusinessValidator<PerishableResource> {

    private final Validator validator;

    /**
     * Constructor for {@code ResourceBusinessValidator} with dependency injection.
     * @param validator the bean validator
     */
    public ResourceBusinessValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates the given resource entity.
     * @param resource the resource entity to validate
     */
    @Override
    protected void doValidate(PerishableResource resource) {
        validator.validate(resource).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateOriginalPricePositive(resource);
    }

    /**
     * Validates the given resource entity for the specified operation.
     * @param entity    the entity to validate
     * @param operation the operation being performed
     */
    @Override
    public void validateOperation(PerishableResource entity, BusinessOperation operation) {
        validate(entity);
    }

    /**
     * Validates that the original price is greater than zero.
     *
     * @param resource the resource entity to validate
     */
    private void validateOriginalPricePositive(PerishableResource resource) {
        if (resource.getOriginalPrice() == null || !resource.getOriginalPrice().isPositive()) {
            addError(
                    "originalPrice must be greater than zero",
                    "originalPrice",
                    resource.getOriginalPrice(),
                    "ORIGINAL_PRICE_NOT_POSITIVE");
        }
    }
}