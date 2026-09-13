package br.com.seudominio.foodrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;
import br.com.seudominio.foodrescue.persistence.repositories.EstablishmentRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

class EstablishmentBusinessValidatorTest {

    private static final String VALID_CNPJ = "11222333000181";

    private final Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

    private Establishment validEstablishment() {
        Establishment establishment = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();
        establishment.setCreationDate(LocalDateTime.now());
        return establishment;
    }

    @Test
    void passesForNewEstablishmentWithUniqueCnpjAndEmail() {
        EstablishmentRepository repository = mock(EstablishmentRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        EstablishmentBusinessValidator validator = new EstablishmentBusinessValidator(beanValidator, repository);

        validator.validateOperation(validEstablishment(), BusinessOperation.CREATE);
    }

    @Test
    void rejectsCnpjWithInvalidCheckDigits() {
        EstablishmentRepository repository = mock(EstablishmentRepository.class);
        when(repository.findByCnpj("11222333000199")).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        EstablishmentBusinessValidator validator = new EstablishmentBusinessValidator(beanValidator, repository);

        Establishment establishment = validEstablishment();
        establishment.setCnpj("11222333000199");

        assertThatThrownBy(() -> validator.validateOperation(establishment, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("INVALID_CNPJ_FORMAT")));
    }

    @Test
    void rejectsCnpjWithRepeatedDigits() {
        EstablishmentRepository repository = mock(EstablishmentRepository.class);
        when(repository.findByCnpj("11111111111111")).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        EstablishmentBusinessValidator validator = new EstablishmentBusinessValidator(beanValidator, repository);

        Establishment establishment = validEstablishment();
        establishment.setCnpj("11111111111111");

        assertThatThrownBy(() -> validator.validateOperation(establishment, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("INVALID_CNPJ_FORMAT")));
    }

    @Test
    void rejectsCnpjAlreadyUsedByAnotherEstablishment() {
        Establishment existing = validEstablishment();
        existing.setId(1L);
        EstablishmentRepository repository = mock(EstablishmentRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        EstablishmentBusinessValidator validator = new EstablishmentBusinessValidator(beanValidator, repository);

        Establishment newEstablishment = validEstablishment();

        assertThatThrownBy(() -> validator.validateOperation(newEstablishment, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CNPJ_ALREADY_EXISTS")));
    }

    @Test
    void rejectsEmailAlreadyUsedByAnotherEstablishment() {
        Establishment existing = validEstablishment();
        existing.setId(1L);
        EstablishmentRepository repository = mock(EstablishmentRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));
        EstablishmentBusinessValidator validator = new EstablishmentBusinessValidator(beanValidator, repository);

        Establishment newEstablishment = validEstablishment();

        assertThatThrownBy(() -> validator.validateOperation(newEstablishment, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    void allowsCnpjAndEmailToMatchTheEntityItself() {
        Establishment existing = validEstablishment();
        existing.setId(1L);
        EstablishmentRepository repository = mock(EstablishmentRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));
        EstablishmentBusinessValidator validator = new EstablishmentBusinessValidator(beanValidator, repository);

        validator.validateOperation(existing, BusinessOperation.UPDATE);
    }

    @Test
    void rejectsBlankName() {
        EstablishmentRepository repository = mock(EstablishmentRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        EstablishmentBusinessValidator validator = new EstablishmentBusinessValidator(beanValidator, repository);

        Establishment establishment = validEstablishment();
        establishment.setName(" ");

        assertThatThrownBy(() -> validator.validateOperation(establishment, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CONSTRAINT_VIOLATION")));
    }
}
