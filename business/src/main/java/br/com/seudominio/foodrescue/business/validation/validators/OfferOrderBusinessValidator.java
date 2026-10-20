package br.com.seudominio.foodrescue.business.validation.validators;

import br.com.seudominio.foodrescue.business.validation.BusinessValidator;
import br.com.seudominio.foodrescue.core.validation.AbstractValidator;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.domain.entities.OfferOrder;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

/**
 * Business validator for {@link OfferOrder} (UC09).
 *
 * <p>Guards the invariants of a consumer reservation: it always references an
 * offer and a consumer, has a positive quantity, and carries a status.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class OfferOrderBusinessValidator extends AbstractValidator<OfferOrder>
        implements BusinessValidator<OfferOrder> {

    private final Validator validator;

    /**
     * Constructor.
     *
     * @param validator the bean validator
     */
    public OfferOrderBusinessValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    protected void doValidate(OfferOrder order) {
        if (order == null) {
            addError("offerOrder must not be null", "offerOrder", null, "NULL_ENTITY");
            return;
        }

        validator.validate(order).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateOffer(order);
        validateConsumer(order);
        validateQuantity(order);
        validateStatus(order);
    }

    @Override
    public void validateOperation(OfferOrder entity, BusinessOperation operation) {
        validate(entity);
    }

    private void validateOffer(OfferOrder order) {
        if (order.getOffer() == null) {
            addError("offerOrder must reference an offer", "offer", null, "ORDER_WITHOUT_OFFER");
        }
    }

    private void validateConsumer(OfferOrder order) {
        if (order.getConsumer() == null) {
            addError("offerOrder must reference a consumer", "consumer", null, "ORDER_WITHOUT_CONSUMER");
        }
    }

    private void validateQuantity(OfferOrder order) {
        if (order.getQuantity() <= 0) {
            addError("quantity must be greater than zero",
                    "quantity", order.getQuantity(), "QUANTITY_NOT_POSITIVE");
        }
    }

    private void validateStatus(OfferOrder order) {
        if (order.getStatus() == null) {
            addError("status must not be null", "status", null, "STATUS_NULL");
        }
    }
}
