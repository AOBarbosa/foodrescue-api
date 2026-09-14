package br.com.seudominio.foodrescue.business.validation.validators;

import br.com.seudominio.foodrescue.business.validation.BusinessValidator;
import br.com.seudominio.foodrescue.core.validation.AbstractValidator;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.domain.entities.Product;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

/**
 * Business validator for {@link Product} entity.
 *
 * @since 1.0.0
 * @author Clovis Medeiros
 */
@Component
public class ProductBusinessValidator extends AbstractValidator<Product> implements BusinessValidator<Product> {

    private final Validator validator;

    /**
     * Constructor for {@code ProductBusinessValidator} with dependency injection.
     * @param validator the bean validator
     */
    public ProductBusinessValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates the given product entity.
     * @param product the product entity to validate
     */
    @Override
    protected void doValidate(Product product) {
        validator.validate(product).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateOriginalPricePositive(product);
    }

    /**
     * Validates the given product entity for the specified operation.
     * @param entity    the entity to validate
     * @param operation the operation being performed
     */
    @Override
    public void validateOperation(Product entity, BusinessOperation operation) {
        validate(entity);
    }

    /**
     * Validates that the original price is greater than zero.
     *
     * @param product the product entity to validate
     */
    private void validateOriginalPricePositive(Product product) {
        if (product.getOriginalPrice() == null || !product.getOriginalPrice().isPositive()) {
            addError(
                    "originalPrice must be greater than zero",
                    "originalPrice",
                    product.getOriginalPrice(),
                    "ORIGINAL_PRICE_NOT_POSITIVE");
        }
    }
}