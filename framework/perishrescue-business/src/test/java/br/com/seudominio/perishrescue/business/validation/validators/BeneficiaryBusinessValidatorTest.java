package br.com.seudominio.perishrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

class BeneficiaryBusinessValidatorTest {

    private final Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

    private Beneficiary validBeneficiary() {
        Beneficiary beneficiary = Beneficiary.builder()
                .name("Maria")
                .document("52998224725")
                .email("maria@example.com")
                .passwordHash("hashed-password")
                .build();
        beneficiary.setCreationDate(LocalDateTime.now());
        return beneficiary;
    }

    @Test
    void passesForNewBeneficiaryWithUniqueEmail() {
        BeneficiaryRepository repository = mock(BeneficiaryRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        BeneficiaryBusinessValidator validator = new BeneficiaryBusinessValidator(beanValidator, repository);

        validator.validateOperation(validBeneficiary(), BusinessOperation.CREATE);
    }

    @Test
    void rejectsEmailAlreadyUsedByAnotherBeneficiary() {
        Beneficiary existing = validBeneficiary();
        existing.setId(1L);
        BeneficiaryRepository repository = mock(BeneficiaryRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.of(existing));
        BeneficiaryBusinessValidator validator = new BeneficiaryBusinessValidator(beanValidator, repository);

        Beneficiary newBeneficiary = validBeneficiary();

        assertThatThrownBy(() -> validator.validateOperation(newBeneficiary, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    void allowsEmailToMatchTheEntityItself() {
        Beneficiary existing = validBeneficiary();
        existing.setId(1L);
        BeneficiaryRepository repository = mock(BeneficiaryRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.of(existing));
        BeneficiaryBusinessValidator validator = new BeneficiaryBusinessValidator(beanValidator, repository);

        validator.validateOperation(existing, BusinessOperation.UPDATE);
    }

    @Test
    void rejectsCpfWithInvalidCheckDigits() {
        BeneficiaryRepository repository = mock(BeneficiaryRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        when(repository.findByDocument("52998224700")).thenReturn(Optional.empty());
        BeneficiaryBusinessValidator validator = new BeneficiaryBusinessValidator(beanValidator, repository);

        Beneficiary beneficiary = validBeneficiary();
        beneficiary.setDocument("52998224700");

        assertThatThrownBy(() -> validator.validateOperation(beneficiary, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("INVALID_CPF")));
    }

    @Test
    void rejectsCpfWithRepeatedDigits() {
        BeneficiaryRepository repository = mock(BeneficiaryRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        when(repository.findByDocument("11111111111")).thenReturn(Optional.empty());
        BeneficiaryBusinessValidator validator = new BeneficiaryBusinessValidator(beanValidator, repository);

        Beneficiary beneficiary = validBeneficiary();
        beneficiary.setDocument("11111111111");

        assertThatThrownBy(() -> validator.validateOperation(beneficiary, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("INVALID_CPF")));
    }

    @Test
    void rejectsCpfAlreadyUsedByAnotherBeneficiary() {
        Beneficiary existing = validBeneficiary();
        existing.setId(1L);
        BeneficiaryRepository repository = mock(BeneficiaryRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        when(repository.findByDocument("52998224725")).thenReturn(Optional.of(existing));
        BeneficiaryBusinessValidator validator = new BeneficiaryBusinessValidator(beanValidator, repository);

        assertThatThrownBy(() -> validator.validateOperation(validBeneficiary(), BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("CPF_ALREADY_EXISTS")));
    }

    @Test
    void rejectsBlankName() {
        BeneficiaryRepository repository = mock(BeneficiaryRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        BeneficiaryBusinessValidator validator = new BeneficiaryBusinessValidator(beanValidator, repository);

        Beneficiary beneficiary = validBeneficiary();
        beneficiary.setName(" ");

        assertThatThrownBy(() -> validator.validateOperation(beneficiary, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class);
    }
}
