package br.com.seudominio.perishrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

class OffererBusinessValidatorTest {

    private static final String VALID_CNPJ = "11222333000181";

    private final Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

    private Offerer validOfferer() {
        Offerer offerer = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();
        offerer.setCreationDate(LocalDateTime.now());
        return offerer;
    }

    @Test
    void passesForNewOffererWithUniqueCnpjAndEmail() {
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        validator.validateOperation(validOfferer(), BusinessOperation.CREATE);
    }

    @Test
    void rejectsACategoryTheInstanceDoesNotAccept() {
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository,
                java.util.List.of("BAKERY", " MARKET "));

        validator.validateOperation(validOfferer(), BusinessOperation.CREATE);

        Offerer typo = validOfferer();
        typo.setCategory("PADAIRA");
        assertThatThrownBy(() -> validator.validateOperation(typo, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("INVALID_CATEGORY")));
    }

    @Test
    void acceptsAnyCategoryWhenTheInstanceListsNone() {
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        Offerer pharmacy = validOfferer();
        pharmacy.setCategory("PHARMACY");

        new OffererBusinessValidator(beanValidator, repository).validateOperation(pharmacy, BusinessOperation.CREATE);
    }

    @Test
    void rejectsOpeningTimeNotBeforeClosingTime() {
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        Offerer offerer = validOfferer();
        offerer.setOpeningTime(LocalTime.of(22, 0));
        offerer.setClosingTime(LocalTime.of(7, 0));

        assertThatThrownBy(() -> validator.validateOperation(offerer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("INVALID_BUSINESS_HOURS")));
    }

    @Test
    void rejectsMissingBusinessHours() {
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        Offerer offerer = validOfferer();
        offerer.setClosingTime(null);

        assertThatThrownBy(() -> validator.validateOperation(offerer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.field()).isEqualTo("closingTime")));
    }

    @Test
    void rejectsCnpjWithInvalidCheckDigits() {
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj("11222333000199")).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        Offerer offerer = validOfferer();
        offerer.setCnpj("11222333000199");

        assertThatThrownBy(() -> validator.validateOperation(offerer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("INVALID_CNPJ_FORMAT")));
    }

    @Test
    void rejectsCnpjWithRepeatedDigits() {
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj("11111111111111")).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        Offerer offerer = validOfferer();
        offerer.setCnpj("11111111111111");

        assertThatThrownBy(() -> validator.validateOperation(offerer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("INVALID_CNPJ_FORMAT")));
    }

    @Test
    void rejectsCnpjAlreadyUsedByAnotherOfferer() {
        Offerer existing = validOfferer();
        existing.setId(1L);
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        Offerer newOfferer = validOfferer();

        assertThatThrownBy(() -> validator.validateOperation(newOfferer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CNPJ_ALREADY_EXISTS")));
    }

    @Test
    void rejectsEmailAlreadyUsedByAnotherOfferer() {
        Offerer existing = validOfferer();
        existing.setId(1L);
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        Offerer newOfferer = validOfferer();

        assertThatThrownBy(() -> validator.validateOperation(newOfferer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    void allowsCnpjAndEmailToMatchTheEntityItself() {
        Offerer existing = validOfferer();
        existing.setId(1L);
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        validator.validateOperation(existing, BusinessOperation.UPDATE);
    }

    @Test
    void rejectsBlankName() {
        OffererRepository repository = mock(OffererRepository.class);
        when(repository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(repository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        OffererBusinessValidator validator = new OffererBusinessValidator(beanValidator, repository);

        Offerer offerer = validOfferer();
        offerer.setName(" ");

        assertThatThrownBy(() -> validator.validateOperation(offerer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CONSTRAINT_VIOLATION")));
    }
}
