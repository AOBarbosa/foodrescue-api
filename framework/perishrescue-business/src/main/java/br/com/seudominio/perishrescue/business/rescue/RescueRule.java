package br.com.seudominio.perishrescue.business.rescue;

import br.com.seudominio.perishrescue.core.validation.ValidationError;

import java.util.Optional;

/**
 * An eligibility or compliance rule a reservation must satisfy.
 *
 * <p><b>Hot spot 3 of the PerishRescue framework.</b> Every {@code RescueRule}
 * bean the instance declares is checked when a beneficiary reserves, all of
 * them, so the beneficiary sees every reason at once (the rules form a chain
 * whose failures are collected into one validation error). Examples:
 * a quota per CPF, pickup within business hours, no active sanitary recall,
 * a minimum lot size.</p>
 *
 * <p>The kernel's own invariants (positive quantity, open offer, enough units)
 * are not rules: they always run and cannot be switched off. The framework
 * ships reusable rules ({@link QuotaPerBeneficiaryRule},
 * {@link PickupWindowRule}, {@link MinimumLotRule}) for instances to declare
 * as beans with their own parameters.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public interface RescueRule {

    /**
     * Checks the reservation against this rule.
     *
     * @param context the offer, beneficiary, quantity, time, extra request data and reservation history
     * @return the violation, or empty if the reservation complies
     */
    Optional<ValidationError> check(RescueContext context);
}
