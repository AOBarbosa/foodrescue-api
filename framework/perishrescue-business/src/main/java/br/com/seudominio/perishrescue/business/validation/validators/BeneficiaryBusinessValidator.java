package br.com.seudominio.perishrescue.business.validation.validators;

import br.com.seudominio.perishrescue.business.validation.BusinessValidator;
import br.com.seudominio.perishrescue.core.validation.AbstractValidator;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Business validator for {@link Beneficiary} entity.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
@Component
public class BeneficiaryBusinessValidator extends AbstractValidator<Beneficiary> implements BusinessValidator<Beneficiary> {

    private final Validator validator;

    private final BeneficiaryRepository beneficiaryRepository;

    /**
     * Constructor for {@code BeneficiaryBusinessValidator} with dependency injection.
     *
     * @param validator           the bean validator
     * @param beneficiaryRepository  the beneficiary repository
     */
    public BeneficiaryBusinessValidator(Validator validator, BeneficiaryRepository beneficiaryRepository) {
        this.validator = validator;
        this.beneficiaryRepository = beneficiaryRepository;
    }

    /**
     * Validates the given beneficiary entity.
     *
     * @param beneficiary the beneficiary entity to validate
     */
    @Override
    protected void doValidate(Beneficiary beneficiary) {
        validator.validate(beneficiary).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateEmailUnique(beneficiary);
        validateDocumentFormat(beneficiary);
        validateDocumentUnique(beneficiary);
    }

    /**
     * Validates that the CPF (already normalized to digits by the mapper) has
     * 11 digits and valid check digits.
     *
     * @param beneficiary the beneficiary entity to validate
     */
    private void validateDocumentFormat(Beneficiary beneficiary) {
        String document = beneficiary.getDocument();
        if (document == null || document.isBlank()) {
            return; // already reported by bean validation
        }
        if (!isValidCpf(document)) {
            addError("invalid CPF", "document", document, "INVALID_CPF");
        }
    }

    /**
     * Validates that no other beneficiary is registered with the same CPF.
     *
     * @param beneficiary the beneficiary entity to validate
     */
    private void validateDocumentUnique(Beneficiary beneficiary) {
        if (beneficiary.getDocument() == null || beneficiary.getDocument().isBlank()) {
            return;
        }

        beneficiaryRepository.findByDocument(beneficiary.getDocument())
                .filter(existing -> !Objects.equals(existing.getId(), beneficiary.getId()))
                .ifPresent(existing -> addError(
                        "CPF already registered",
                        "document",
                        beneficiary.getDocument(),
                        "CPF_ALREADY_EXISTS"));
    }

    /**
     * Checks whether the given CPF (digits only) is valid.
     *
     * @param cpf the CPF to check
     * @return {@code true} if the CPF has 11 digits, is not a repeated digit
     *         sequence and both check digits match
     */
    private static boolean isValidCpf(String cpf) {
        if (!cpf.matches("\\d{11}") || cpf.chars().distinct().count() == 1) {
            return false;
        }
        return cpf.charAt(9) - '0' == cpfCheckDigit(cpf, 9)
                && cpf.charAt(10) - '0' == cpfCheckDigit(cpf, 10);
    }

    private static int cpfCheckDigit(String cpf, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (cpf.charAt(i) - '0') * (length + 1 - i);
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    /**
     * Validates the given beneficiary entity for the specified business operation.
     *
     * @param entity    the beneficiary entity to validate
     * @param operation the business operation to validate against
     */
    @Override
    public void validateOperation(Beneficiary entity, BusinessOperation operation) {
        errors.clear();
        doValidate(entity);

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed: " + getMessagesErrors(), errors);
        }
    }

    /**
     * Validates that the email of the beneficiary is unique.
     *
     * @param beneficiary the beneficiary entity to validate
     */
    private void validateEmailUnique(Beneficiary beneficiary) {
        if (beneficiary.getEmail() == null || beneficiary.getEmail().isBlank()) {
            return;
        }

        beneficiaryRepository.findByEmail(beneficiary.getEmail())
                .filter(existing -> !Objects.equals(existing.getId(), beneficiary.getId()))
                .ifPresent(existing -> addError(
                        "email already registered",
                        "email",
                        beneficiary.getEmail(),
                        "EMAIL_ALREADY_EXISTS"));
    }
}
