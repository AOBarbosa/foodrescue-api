package br.com.seudominio.perishrescue.business.pricing;

import br.com.seudominio.perishrescue.core.percentage.Percentage;

/**
 * How a perishable resource loses value as its useful life runs out.
 *
 * <p><b>Hot spot 2 of the PerishRescue framework (Strategy).</b> The kernel
 * asks the policy for a discount whenever it prices an offer (on publishing,
 * on each periodic repricing) and when a beneficiary reserves (the context
 * then carries the beneficiary, so a policy may grant a beneficiary-specific
 * discount, e.g. a donation to a charity). The kernel applies the larger of
 * this discount and any discount the offerer accepted from a recommendation,
 * never raises an offer's price, and caps the discount at
 * {@code perishrescue.pricing.max-discount}.</p>
 *
 * <p>The framework ships {@link StepwiseDepreciationPolicy}, configurable
 * by properties; an instance replaces it by declaring its own
 * {@code PricingPolicy} bean.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public interface PricingPolicy {

    /**
     * The discount the resource deserves in the given context.
     *
     * @param context the resource, the beneficiary (if any) and the current time
     * @return the discount, {@code 0%} for none
     */
    Percentage discountFor(PricingContext context);
}
