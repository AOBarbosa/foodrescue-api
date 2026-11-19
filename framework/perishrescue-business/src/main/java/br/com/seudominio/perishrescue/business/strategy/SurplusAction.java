package br.com.seudominio.perishrescue.business.strategy;

/**
 * Alternative actions for a resource still at risk after a discount (UC10).
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
public enum SurplusAction {

    /** Deepen the discount. */
    ADDITIONAL_DISCOUNT,

    /** Show the resource to beneficiaries with priority. */
    PRIORITIZE,

    /**
     * Take the remaining units off sale and donate them (e.g. medicines to a
     * charity, flowers for compost). Only suggested when the instance enables
     * it, after a priority exposure did not solve the surplus.
     */
    DONATE
}
