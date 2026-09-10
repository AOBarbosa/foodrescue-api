package br.com.seudominio.foodrescue.domain.builders;

import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.domain.entities.AiRecommendation;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;

import java.time.LocalDateTime;

/**
 * Builder class for creating instances of {@link AiRecommendation}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class AiRecommendationBuilder {

    private Long id;
    private Product product;
    private RecommendationType type;
    private Percentage suggestedPercentage;
    private RecommendationStatus status;
    private Long previousRecommendationId;
    private LocalDateTime respondedAt;

    /**
     * Sets the ID of the recommendation.
     *
     * @param id the ID of the recommendation
     * @return the current instance of {@link AiRecommendationBuilder}
     */
    public AiRecommendationBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the product this recommendation is about.
     *
     * @param product the product this recommendation is about
     * @return the current instance of {@link AiRecommendationBuilder}
     */
    public AiRecommendationBuilder product(Product product) {
        this.product = product;
        return this;
    }

    /**
     * Sets the kind of recommendation.
     *
     * @param type the kind of recommendation
     * @return the current instance of {@link AiRecommendationBuilder}
     */
    public AiRecommendationBuilder type(RecommendationType type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the suggested discount percentage.
     *
     * @param suggestedPercentage the suggested discount percentage
     * @return the current instance of {@link AiRecommendationBuilder}
     */
    public AiRecommendationBuilder suggestedPercentage(Percentage suggestedPercentage) {
        this.suggestedPercentage = suggestedPercentage;
        return this;
    }

    /**
     * Sets the lifecycle status of the recommendation. Defaults to {@link RecommendationStatus#PENDING} when not set.
     *
     * @param status the lifecycle status of the recommendation
     * @return the current instance of {@link AiRecommendationBuilder}
     */
    public AiRecommendationBuilder status(RecommendationStatus status) {
        this.status = status;
        return this;
    }

    /**
     * Sets the identifier of the recommendation this one follows.
     *
     * @param previousRecommendationId the identifier of the recommendation this one follows
     * @return the current instance of {@link AiRecommendationBuilder}
     */
    public AiRecommendationBuilder previousRecommendationId(Long previousRecommendationId) {
        this.previousRecommendationId = previousRecommendationId;
        return this;
    }

    /**
     * Sets the moment the establishment responded to the recommendation.
     *
     * @param respondedAt the moment the establishment responded to the recommendation
     * @return the current instance of {@link AiRecommendationBuilder}
     */
    public AiRecommendationBuilder respondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
        return this;
    }

    /**
     * Builds and returns an instance of {@link AiRecommendation} with the set properties.
     *
     * @return a new instance of {@link AiRecommendation}
     */
    public AiRecommendation build() {
        AiRecommendation aiRecommendation = new AiRecommendation();
        aiRecommendation.setId(id);
        aiRecommendation.setProduct(product);
        aiRecommendation.setType(type);
        aiRecommendation.setSuggestedPercentage(suggestedPercentage);
        aiRecommendation.setStatus(status != null ? status : RecommendationStatus.PENDING);
        aiRecommendation.setPreviousRecommendationId(previousRecommendationId);
        aiRecommendation.setRespondedAt(respondedAt);
        return aiRecommendation;
    }
}
