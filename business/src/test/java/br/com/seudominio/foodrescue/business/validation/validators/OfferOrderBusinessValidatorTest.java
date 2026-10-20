package br.com.seudominio.foodrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.OfferOrder;
import br.com.seudominio.foodrescue.domain.enums.OfferOrderStatus;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class OfferOrderBusinessValidatorTest {

    private OfferOrderBusinessValidator validator;

    @BeforeEach
    void setUp() {
        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any())).thenReturn(Collections.emptySet());
        validator = new OfferOrderBusinessValidator(beanValidator);
    }

    private OfferOrder validOrder() {
        return OfferOrder.builder()
                .offer(new Offer())
                .consumer(new Consumer())
                .quantity(2)
                .status(OfferOrderStatus.RESERVED)
                .build();
    }

    @Test
    void acceptsValidOrder() {
        assertThatCode(() -> validator.validateOperation(validOrder(), BusinessOperation.CREATE))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNullOrder() {
        assertThatThrownBy(() -> validator.validateOperation(null, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsOrderWithoutOffer() {
        OfferOrder order = validOrder();
        order.setOffer(null);

        assertThatThrownBy(() -> validator.validateOperation(order, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("offerOrder must reference an offer");
    }

    @Test
    void rejectsOrderWithoutConsumer() {
        OfferOrder order = validOrder();
        order.setConsumer(null);

        assertThatThrownBy(() -> validator.validateOperation(order, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("offerOrder must reference a consumer");
    }

    @Test
    void rejectsOrderWithNonPositiveQuantity() {
        OfferOrder order = validOrder();
        order.setQuantity(0);

        assertThatThrownBy(() -> validator.validateOperation(order, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("quantity must be greater than zero");
    }

    @Test
    void rejectsOrderWithoutStatus() {
        OfferOrder order = validOrder();
        order.setStatus(null);

        assertThatThrownBy(() -> validator.validateOperation(order, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("status must not be null");
    }
}
