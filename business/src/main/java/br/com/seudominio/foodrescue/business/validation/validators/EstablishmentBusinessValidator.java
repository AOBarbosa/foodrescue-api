package br.com.seudominio.foodrescue.business.validation.validators;

import br.com.seudominio.foodrescue.business.validation.BusinessValidator;
import br.com.seudominio.foodrescue.core.validation.AbstractValidator;
import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.persistence.repositories.EstablishmentRepository;

import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Business validator for {@link Establishment} entity.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
@Component
public class EstablishmentBusinessValidator extends AbstractValidator<Establishment>
        implements BusinessValidator<Establishment> {

    private static final int[] FIRST_CHECK_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_CHECK_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private final Validator validator;

    private final EstablishmentRepository establishmentRepository;

    /**
     * Constructor for {@code EstablishmentBusinessValidator} with dependency injection.
     *
     * @param validator               the bean validator
     * @param establishmentRepository the establishment repository
     */
    public EstablishmentBusinessValidator(Validator validator, EstablishmentRepository establishmentRepository) {
        this.validator = validator;
        this.establishmentRepository = establishmentRepository;
    }

    /**
     * Validates the given establishment entity.
     *
     * @param establishment the establishment entity to validate
     */
    @Override
    protected void doValidate(Establishment establishment) {
        validator.validate(establishment).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateCnpjFormat(establishment);
        validateCnpjUnique(establishment);
        validateEmailUnique(establishment);
    }

    /**
     * Validates the given establishment entity for the specified business operation.
     *
     * @param entity    the establishment entity to validate
     * @param operation the business operation to validate against
     */
    @Override
    public void validateOperation(Establishment entity, BusinessOperation operation) {
        errors.clear();
        doValidate(entity);

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed: " + getMessagesErrors(), errors);
        }
    }

    /**
     * Validates that the CNPJ of the establishment has a valid format, i.e. 14
     * digits with valid check digits (modulo 11), rejecting repeated-digit
     * sequences (e.g. {@code 00000000000000}).
     *
     * @param establishment the establishment entity to validate
     */
    private void validateCnpjFormat(Establishment establishment) {
        String cnpj = establishment.getCnpj();
        if (cnpj == null || cnpj.isBlank()) {
            return;
        }

        if (!isValidCnpj(cnpj)) {
            addError("invalid CNPJ", "cnpj", cnpj, "INVALID_CNPJ_FORMAT");
        }
    }

    /**
     * Validates that the CNPJ of the establishment is unique.
     *
     * @param establishment the establishment entity to validate
     */
    private void validateCnpjUnique(Establishment establishment) {
        if (establishment.getCnpj() == null || establishment.getCnpj().isBlank()) {
            return;
        }

        establishmentRepository.findByCnpj(establishment.getCnpj())
                .filter(existing -> !Objects.equals(existing.getId(), establishment.getId()))
                .ifPresent(existing -> addError(
                        "CNPJ already registered",
                        "cnpj",
                        establishment.getCnpj(),
                        "CNPJ_ALREADY_EXISTS"));
    }

    /**
     * Validates that the email of the establishment is unique.
     *
     * @param establishment the establishment entity to validate
     */
    private void validateEmailUnique(Establishment establishment) {
        if (establishment.getEmail() == null || establishment.getEmail().isBlank()) {
            return;
        }

        establishmentRepository.findByEmail(establishment.getEmail())
                .filter(existing -> !Objects.equals(existing.getId(), establishment.getId()))
                .ifPresent(existing -> addError(
                        "email already registered",
                        "email",
                        establishment.getEmail(),
                        "EMAIL_ALREADY_EXISTS"));
    }

    /**
     * Checks whether the given CNPJ (already normalized to digits only) is
     * valid: exactly 14 digits, not a repeated-digit sequence, and its two
     * check digits match the modulo-11 algorithm.
     *
     * @param cnpj the CNPJ to check
     * @return {@code true} if the CNPJ is valid
     */
    private static boolean isValidCnpj(String cnpj) {
        if (!cnpj.matches("\\d{14}") || cnpj.chars().distinct().count() == 1) {
            return false;
        }

        int firstCheckDigit = calculateCheckDigit(cnpj.substring(0, 12), FIRST_CHECK_DIGIT_WEIGHTS);
        int secondCheckDigit = calculateCheckDigit(cnpj.substring(0, 12) + firstCheckDigit, SECOND_CHECK_DIGIT_WEIGHTS);

        return cnpj.charAt(12) - '0' == firstCheckDigit && cnpj.charAt(13) - '0' == secondCheckDigit;
    }

    /**
     * Calculates one CNPJ check digit from the given base digits and weights.
     *
     * @param digits  the base digits
     * @param weights the weights to apply, same length as {@code digits}
     * @return the calculated check digit
     */
    private static int calculateCheckDigit(String digits, int[] weights) {
        int sum = 0;
        for (int i = 0; i < digits.length(); i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
