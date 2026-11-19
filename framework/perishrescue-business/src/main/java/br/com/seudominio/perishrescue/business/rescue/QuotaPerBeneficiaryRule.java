package br.com.seudominio.perishrescue.business.rescue;

import br.com.seudominio.perishrescue.core.validation.ValidationError;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Reusable {@link RescueRule}: a beneficiary (one CPF) may reserve at most
 * {@code maxUnits} units per calendar day. FoodRescue uses 3 units/day;
 * PharmaRescue would use 2 boxes.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public class QuotaPerBeneficiaryRule implements RescueRule {

    private final int maxUnits;

    /**
     * Creates the rule.
     *
     * @param maxUnits the most units one beneficiary may reserve per day
     */
    public QuotaPerBeneficiaryRule(int maxUnits) {
        if (maxUnits <= 0) {
            throw new IllegalArgumentException("maxUnits must be positive");
        }
        this.maxUnits = maxUnits;
    }

    @Override
    public Optional<ValidationError> check(RescueContext context) {
        LocalDateTime startOfDay = context.now().toLocalDate().atStartOfDay();
        int alreadyReserved = context.history()
                .unitsReservedBy(context.beneficiary().getId(), startOfDay, startOfDay.plusDays(1));
        if (alreadyReserved + context.quantity() <= maxUnits) {
            return Optional.empty();
        }
        return Optional.of(new ValidationError(
                String.format("daily limit of %d units per CPF exceeded: %d already reserved today",
                        maxUnits, alreadyReserved),
                "quantity", context.quantity(), "RESCUE_QUOTA_EXCEEDED"));
    }
}
