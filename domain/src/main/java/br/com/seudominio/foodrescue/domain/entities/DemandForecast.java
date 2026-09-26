package br.com.seudominio.foodrescue.domain.entities;

import br.com.seudominio.foodrescue.domain.builders.DemandForecastBuilder;
import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

import jakarta.persistence.Column;
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
 * DemandForecast. This class represents how many units of a {@link Product}
 * are expected to be sold between the moment of calculation and the end of
 * the business day, based on its {@link Sale} history (UC05). It feeds the
 * waste-risk analysis (UC06).
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Entity
@Audited
@Table(name = "demand_forecasts")
@SuppressWarnings("serial")
public class DemandForecast extends AbstractEntity {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_DEMAND_FORECAST")
    @SequenceGenerator(name = "SEQ_DEMAND_FORECAST", sequenceName = "seq_demand_forecast", allocationSize = 1)
    private Long id;

    /**
     * The product this forecast is about.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Units expected to be sold until the end of the business day.
     */
    @Column(name = "predicted_quantity", nullable = false)
    private int predictedQuantity;

    /**
     * Stock quantity of the product at the moment of calculation.
     */
    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    /**
     * Confidence level of the forecast.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ForecastConfidence confidence;

    /**
     * Number of past sales considered in the calculation.
     */
    @Column(name = "sample_size", nullable = false)
    private int sampleSize;

    /**
     * Moment the forecast was calculated.
     */
    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    /**
     * End of the business day the forecast refers to.
     */
    @Column(name = "forecast_until", nullable = false)
    private LocalDateTime forecastUntil;

    /**
     * Default constructor.
     */
    public DemandForecast() {
        super();
    }

    /**
     * Returns a new instance of the DemandForecastBuilder for building DemandForecast objects.
     *
     * @return A new DemandForecastBuilder instance.
     */
    public static DemandForecastBuilder builder() {
        return new DemandForecastBuilder();
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

    public int getPredictedQuantity() {
        return predictedQuantity;
    }

    public void setPredictedQuantity(int predictedQuantity) {
        this.predictedQuantity = predictedQuantity;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public ForecastConfidence getConfidence() {
        return confidence;
    }

    public void setConfidence(ForecastConfidence confidence) {
        this.confidence = confidence;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public LocalDateTime getForecastUntil() {
        return forecastUntil;
    }

    public void setForecastUntil(LocalDateTime forecastUntil) {
        this.forecastUntil = forecastUntil;
    }

    @Override
    public String toString() {
        return "DemandForecast [id=" + id + ", productId=" + (product == null ? null : product.getId())
                + ", predictedQuantity=" + predictedQuantity + ", confidence=" + confidence + "]";
    }
}
