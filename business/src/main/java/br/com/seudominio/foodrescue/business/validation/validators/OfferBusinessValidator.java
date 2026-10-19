package br.com.seudominio.foodrescue.business.validation.validators;

import br.com.seudominio.foodrescue.business.validation.BusinessValidator;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.validation.AbstractValidator;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.Product;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

/**
 * Business validator for {@link Offer} (UC08).
 *
 * <p>Guards the invariants of a published offer: it always has a product, a
 * positive price that never exceeds the product's list price, at least one
 * unit, and a deadline still in the future.</p>
 *
 * @author Hugo Jose
 * @since 1.0.0
 */
@Component
public class OfferBusinessValidator extends AbstractValidator<Offer> implements BusinessValidator<Offer> {

    private final Validator validator;
    private final TimeProvider timeProvider;

    /**
     * Constructor.
     *
     * @param validator    the bean validator
     * @param timeProvider the current date and time provider
     */
    public OfferBusinessValidator(Validator validator, TimeProvider timeProvider) {
        this.validator = validator;
        this.timeProvider = timeProvider;
    }

    @Override
    protected void doValidate(Offer offer) {
        validator.validate(offer).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateProduct(offer);
        validateDiscountedPrice(offer);
        validateAvailableQuantity(offer);
        validateExpiresAt(offer);
    }

    @Override
    public void validateOperation(Offer entity, BusinessOperation operation) {
        validate(entity);
    }

    private void validateProduct(Offer offer) {
        if (offer.getProduct() == null) {
            addError("offer must reference a product", "product", null, "OFFER_WITHOUT_PRODUCT");
        }
    }

    private void validateDiscountedPrice(Offer offer) {
        if (offer.getDiscountedPrice() == null || !offer.getDiscountedPrice().isPositive()) {
            addError("discountedPrice must be greater than zero",
                    "discountedPrice", offer.getDiscountedPrice(), "DISCOUNTED_PRICE_NOT_POSITIVE");
            return;
        }

        Product product = offer.getProduct();
        if (product != null && product.getOriginalPrice() != null
                && offer.getDiscountedPrice().isGreaterThan(product.getOriginalPrice())) {
            addError("discountedPrice must not be greater than the product's original price",
                    "discountedPrice", offer.getDiscountedPrice(), "DISCOUNTED_PRICE_ABOVE_ORIGINAL");
        }
    }

    private void validateAvailableQuantity(Offer offer) {
        if (offer.getAvailableQuantity() <= 0) {
            addError("availableQuantity must be greater than zero",
                    "availableQuantity", offer.getAvailableQuantity(), "OFFER_WITHOUT_QUANTITY");
        }
    }

    private void validateExpiresAt(Offer offer) {
        if (offer.getExpiresAt() == null) {
            addError("expiresAt must be informed", "expiresAt", null, "OFFER_WITHOUT_DEADLINE");
            return;
        }
        if (!offer.getExpiresAt().isAfter(timeProvider.now())) {
            addError("expiresAt must be in the future",
                    "expiresAt", offer.getExpiresAt(), "OFFER_DEADLINE_IN_THE_PAST");
        }
    }
}
