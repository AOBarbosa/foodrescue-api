package br.com.seudominio.foodrescue.business.validation.validators;

import br.com.seudominio.foodrescue.business.validation.BusinessValidator;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.validation.AbstractValidator;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.domain.entities.Sale;

import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

/**
 * Business validator for {@link Sale} entity.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class SaleBusinessValidator extends AbstractValidator<Sale> implements BusinessValidator<Sale> {

    private final Validator validator;
    private final TimeProvider timeProvider;

    /**
     * Constructor for {@code SaleBusinessValidator}.
     *
     * @param validator    the bean validator
     * @param timeProvider the time provider
     */
    public SaleBusinessValidator(Validator validator, TimeProvider timeProvider) {
        this.validator = validator;
        this.timeProvider = timeProvider;
    }

    @Override
    protected void doValidate(Sale sale) {
        if (sale == null) {
            addError("sale must not be null", "sale", null, "NULL_ENTITY");
            return;
        }

        validator.validate(sale).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateQuantity(sale);
        validateUnitPrice(sale);
        validateSoldAt(sale);
    }

    @Override
    public void validateOperation(Sale entity, BusinessOperation operation) {
        validate(entity);
    }

    private void validateQuantity(Sale sale) {
        if (sale.getQuantity() <= 0) {
            addError(
                    "quantity must be greater than zero",
                    "quantity",
                    sale.getQuantity(),
                    "QUANTITY_NOT_POSITIVE"
            );
        }
    }

    private void validateUnitPrice(Sale sale) {
        if (sale.getUnitPrice() == null || !sale.getUnitPrice().isPositive()) {
            addError(
                    "unitPrice must be greater than zero",
                    "unitPrice",
                    sale.getUnitPrice(),
                    "UNIT_PRICE_NOT_POSITIVE"
            );
        }
    }

    private void validateSoldAt(Sale sale) {
        if (sale.getSoldAt() == null) {
            addError(
                    "soldAt must not be null",
                    "soldAt",
                    null,
                    "SOLD_AT_NULL"
            );
        } else if (sale.getSoldAt().isAfter(timeProvider.now())) {
            addError(
                    "soldAt must not be in the future",
                    "soldAt",
                    sale.getSoldAt(),
                    "SOLD_AT_IN_FUTURE"
            );
        }
    }
}
