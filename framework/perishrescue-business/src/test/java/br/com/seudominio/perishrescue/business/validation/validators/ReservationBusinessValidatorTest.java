package br.com.seudominio.perishrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.Reservation;
import br.com.seudominio.perishrescue.domain.enums.ReservationStatus;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

class ReservationBusinessValidatorTest {

    private ReservationBusinessValidator validator;

    @BeforeEach
    void setUp() {
        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any())).thenReturn(Collections.emptySet());
        validator = new ReservationBusinessValidator(beanValidator);
    }

    private Reservation validOrder() {
        return Reservation.builder()
                .offer(new Offer())
                .beneficiary(new Beneficiary())
                .quantity(2)
                .status(ReservationStatus.RESERVED)
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
        Reservation order = validOrder();
        order.setOffer(null);

        assertThatThrownBy(() -> validator.validateOperation(order, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reservation must reference an offer");
    }

    @Test
    void rejectsOrderWithoutBeneficiary() {
        Reservation order = validOrder();
        order.setBeneficiary(null);

        assertThatThrownBy(() -> validator.validateOperation(order, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("reservation must reference a beneficiary");
    }

    @Test
    void rejectsOrderWithNonPositiveQuantity() {
        Reservation order = validOrder();
        order.setQuantity(0);

        assertThatThrownBy(() -> validator.validateOperation(order, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("quantity must be greater than zero");
    }

    @Test
    void rejectsOrderWithoutStatus() {
        Reservation order = validOrder();
        order.setStatus(null);

        assertThatThrownBy(() -> validator.validateOperation(order, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("status must not be null");
    }
}
