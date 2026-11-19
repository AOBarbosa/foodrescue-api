package br.com.seudominio.perishrescue.business.validation.validators;

import br.com.seudominio.perishrescue.business.validation.BusinessValidator;
import br.com.seudominio.perishrescue.core.validation.AbstractValidator;
import br.com.seudominio.perishrescue.core.validation.BusinessOperation;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;

import jakarta.validation.Validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Business validator for {@link Offerer} entity.
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
@Component
public class OffererBusinessValidator extends AbstractValidator<Offerer>
        implements BusinessValidator<Offerer> {

    private static final int[] FIRST_CHECK_DIGIT_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_CHECK_DIGIT_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private final Validator validator;

    private final OffererRepository offererRepository;

    private final Set<String> allowedCategories;

    /**
     * Constructor accepting any category.
     *
     * @param validator         the bean validator
     * @param offererRepository the offerer repository
     */
    public OffererBusinessValidator(Validator validator, OffererRepository offererRepository) {
        this(validator, offererRepository, List.of());
    }

    /**
     * Constructor for {@code OffererBusinessValidator} with dependency injection.
     *
     * @param validator         the bean validator
     * @param offererRepository the offerer repository
     * @param allowedCategories the categories the instance accepts
     *                          ({@code perishrescue.offerer.categories}); empty accepts any
     */
    @Autowired
    public OffererBusinessValidator(
            Validator validator,
            OffererRepository offererRepository,
            @Value("${perishrescue.offerer.categories:}") List<String> allowedCategories) {
        this.validator = validator;
        this.offererRepository = offererRepository;
        this.allowedCategories = allowedCategories == null ? Set.of() : allowedCategories.stream()
                .map(String::trim)
                .filter(category -> !category.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Validates the given offerer entity.
     *
     * @param offerer the offerer entity to validate
     */
    @Override
    protected void doValidate(Offerer offerer) {
        validator.validate(offerer).forEach(v -> addError(
                v.getMessage(),
                v.getPropertyPath().toString(),
                v.getInvalidValue(),
                "CONSTRAINT_VIOLATION"));

        validateCnpjFormat(offerer);
        validateCnpjUnique(offerer);
        validateEmailUnique(offerer);
        validateBusinessHours(offerer);
        validateCategory(offerer);
    }

    /**
     * Validates the category against the ones the instance accepts
     * ({@code perishrescue.offerer.categories}); any category passes when the
     * instance lists none.
     *
     * @param offerer the offerer entity to validate
     */
    private void validateCategory(Offerer offerer) {
        String category = offerer.getCategory();
        if (allowedCategories.isEmpty() || category == null || category.isBlank()) {
            return; // blank is already reported by bean validation
        }
        if (!allowedCategories.contains(category)) {
            addError("category must be one of " + new TreeSet<>(allowedCategories),
                    "category", category, "INVALID_CATEGORY");
        }
    }

    /**
     * Validates that the offerer opens before it closes. Overnight business
     * hours (closing after midnight) are not supported.
     *
     * @param offerer the offerer entity to validate
     */
    private void validateBusinessHours(Offerer offerer) {
        if (offerer.getOpeningTime() == null || offerer.getClosingTime() == null) {
            return; // already reported by bean validation
        }
        if (!offerer.getOpeningTime().isBefore(offerer.getClosingTime())) {
            addError("openingTime must be before closingTime",
                    "openingTime", offerer.getOpeningTime(), "INVALID_BUSINESS_HOURS");
        }
    }

    /**
     * Validates the given offerer entity for the specified business operation.
     *
     * @param entity    the offerer entity to validate
     * @param operation the business operation to validate against
     */
    @Override
    public void validateOperation(Offerer entity, BusinessOperation operation) {
        errors.clear();
        doValidate(entity);

        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed: " + getMessagesErrors(), errors);
        }
    }

    /**
     * Validates that the CNPJ of the offerer has a valid format, i.e. 14
     * digits with valid check digits (modulo 11), rejecting repeated-digit
     * sequences (e.g. {@code 00000000000000}).
     *
     * @param offerer the offerer entity to validate
     */
    private void validateCnpjFormat(Offerer offerer) {
        String cnpj = offerer.getCnpj();
        if (cnpj == null || cnpj.isBlank()) {
            return;
        }

        if (!isValidCnpj(cnpj)) {
            addError("invalid CNPJ", "cnpj", cnpj, "INVALID_CNPJ_FORMAT");
        }
    }

    /**
     * Validates that the CNPJ of the offerer is unique.
     *
     * @param offerer the offerer entity to validate
     */
    private void validateCnpjUnique(Offerer offerer) {
        if (offerer.getCnpj() == null || offerer.getCnpj().isBlank()) {
            return;
        }

        offererRepository.findByCnpj(offerer.getCnpj())
                .filter(existing -> !Objects.equals(existing.getId(), offerer.getId()))
                .ifPresent(existing -> addError(
                        "CNPJ already registered",
                        "cnpj",
                        offerer.getCnpj(),
                        "CNPJ_ALREADY_EXISTS"));
    }

    /**
     * Validates that the email of the offerer is unique.
     *
     * @param offerer the offerer entity to validate
     */
    private void validateEmailUnique(Offerer offerer) {
        if (offerer.getEmail() == null || offerer.getEmail().isBlank()) {
            return;
        }

        offererRepository.findByEmail(offerer.getEmail())
                .filter(existing -> !Objects.equals(existing.getId(), offerer.getId()))
                .ifPresent(existing -> addError(
                        "email already registered",
                        "email",
                        offerer.getEmail(),
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
