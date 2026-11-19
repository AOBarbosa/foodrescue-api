package br.com.seudominio.perishrescue.business.validation.validators;

import br.com.seudominio.perishrescue.business.validation.BusinessValidator;
import br.com.seudominio.perishrescue.core.validation.AbstractValidator;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.domain.entities.Reservation;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

/**
 * Business validator for {@link Reservation} (UC09).
 *
 * <p>Guards the invariants of a beneficiary reservation: it always references an
 * offer and a beneficiary, has a positive quantity, and carries a status.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class ReservationBusinessValidator extends AbstractValidator<Reservation>
        implements BusinessValidator<Reservation> {

    private final Validator validator;

    /**
     * Constructor.
     *
     * @param validator the bean validator
     */
    public ReservationBusinessValidator(Validator validator) {
        this.validator = validator;
    }

    @Override
    protected void doValidate(Reservation order) {
        if (order == null) {
            addError("reservation must not be null", "reservation", null, "NULL_ENTITY");
            return;
        }

        validator.validate(order).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateOffer(order);
        validateBeneficiary(order);
        validateQuantity(order);
        validateStatus(order);
    }

    @Override
    public void validateOperation(Reservation entity, BusinessOperation operation) {
        validate(entity);
    }

    private void validateOffer(Reservation order) {
        if (order.getOffer() == null) {
            addError("reservation must reference an offer", "offer", null, "ORDER_WITHOUT_OFFER");
        }
    }

    private void validateBeneficiary(Reservation order) {
        if (order.getBeneficiary() == null) {
            addError("reservation must reference a beneficiary", "beneficiary", null, "ORDER_WITHOUT_BENEFICIARY");
        }
    }

    private void validateQuantity(Reservation order) {
        if (order.getQuantity() <= 0) {
            addError("quantity must be greater than zero",
                    "quantity", order.getQuantity(), "QUANTITY_NOT_POSITIVE");
        }
    }

    private void validateStatus(Reservation order) {
        if (order.getStatus() == null) {
            addError("status must not be null", "status", null, "STATUS_NULL");
        }
    }
}
