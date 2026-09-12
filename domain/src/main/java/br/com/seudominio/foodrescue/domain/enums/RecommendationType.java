package br.com.seudominio.foodrescue.domain.enums;

import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;

/**
 * Kind of suggestion carried by an {@link AiRecommendation}: a discount on the
 * current price (UC07) or a destination for surplus that is still at risk
 * after a discount (UC10).
 *
 * @since 1.0.0
 * @author Andre Barbosa
 */
public enum RecommendationType {

    DISCOUNT,
    SURPLUS_DESTINATION
}
