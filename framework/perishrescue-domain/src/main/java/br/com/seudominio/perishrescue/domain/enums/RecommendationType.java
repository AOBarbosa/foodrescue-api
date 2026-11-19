package br.com.seudominio.perishrescue.domain.enums;

import br.com.seudominio.perishrescue.domain.entities.AiRecommendation;

/**
 * Kind of suggestion carried by an {@link AiRecommendation}: a discount on the
 * current price (UC07) or a destination for surplus that is still at risk
 * after a discount (UC10).
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public enum RecommendationType {

    /** Suggests a discount on the resource's price. */
    DISCOUNT,

    /** Suggests showing the resource to beneficiaries with priority. */
    SURPLUS_DESTINATION,

    /** Suggests taking the remaining units off sale and donating them. */
    DONATION
}
