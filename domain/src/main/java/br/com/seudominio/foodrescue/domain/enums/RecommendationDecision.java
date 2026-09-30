package br.com.seudominio.foodrescue.domain.enums;

/**
 * What an establishment decided about a pending discount recommendation (UC07).
 *
 * <p>Not persisted: it is the intent carried by the request, which the service
 * turns into a {@link RecommendationStatus}.</p>
 *
 * @since 1.0.0
 * @author Hugo Jose
 */
public enum RecommendationDecision {

    /** Apply the suggested percentage as it is. */
    ACCEPT,

    /** Apply a different percentage, chosen by the establishment. */
    ADJUST,

    /** Apply nothing; the price stays untouched. */
    REFUSE
}
