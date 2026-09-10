package br.com.seudominio.foodrescue.domain.entities;

import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.domain.builders.AiRecommendationBuilder;
import br.com.seudominio.foodrescue.domain.converter.PercentageConverter;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.envers.Audited;

import java.time.LocalDateTime;

/**
 * AiRecommendation. This class represents an AI-generated suggestion for a
 * {@link Product}: either a discount (UC07) or a surplus destination once a
 * discount alone isn't enough (UC10). {@code previousRecommendationId} lets
 * a chain of recommendations for the same product be traced (UC10).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "ai_recommendations")
@SuppressWarnings("serial")
public class AiRecommendation extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_AI_RECOMMENDATION")
    @SequenceGenerator(name = "SEQ_AI_RECOMMENDATION", sequenceName = "seq_ai_recommendation", allocationSize = 1)
    private Long id;

    /**
     * The product this recommendation is about.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Kind of recommendation.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType type;

    /**
     * Suggested discount percentage, if applicable.
     */
    @Convert(converter = PercentageConverter.class)
    @Column(name = "suggested_percentage")
    private Percentage suggestedPercentage;

    /**
     * Lifecycle status of the recommendation.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationStatus status;

    /**
     * Identifier of the recommendation this one follows, if any.
     */
    @Column(name = "previous_recommendation_id")
    private Long previousRecommendationId;

    /**
     * Moment the establishment responded to the recommendation, if any.
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    /**
     * Default constructor.
     */
    public AiRecommendation() {
        super();
    }

    /**
     * Returns a new instance of the AiRecommendationBuilder for building AiRecommendation objects.
     *
     * @return A new AiRecommendationBuilder instance.
     */
    public static AiRecommendationBuilder builder() {
        return new AiRecommendationBuilder();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public RecommendationType getType() {
        return type;
    }

    public void setType(RecommendationType type) {
        this.type = type;
    }

    public Percentage getSuggestedPercentage() {
        return suggestedPercentage;
    }

    public void setSuggestedPercentage(Percentage suggestedPercentage) {
        this.suggestedPercentage = suggestedPercentage;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public void setStatus(RecommendationStatus status) {
        this.status = status;
    }

    public Long getPreviousRecommendationId() {
        return previousRecommendationId;
    }

    public void setPreviousRecommendationId(Long previousRecommendationId) {
        this.previousRecommendationId = previousRecommendationId;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    @Override
    public String toString() {
        return "AiRecommendation [id=" + id + ", productId=" + (product == null ? null : product.getId())
                + ", type=" + type + ", status=" + status + "]";
    }
}
