package br.com.seudominio.foodrescue.domain.builders;

import br.com.seudominio.foodrescue.domain.entities.DemandForecast;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.ForecastConfidence;

import java.time.LocalDateTime;

/**
 * Builder class for creating instances of {@link DemandForecast}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
public class DemandForecastBuilder {

    private Long id;
    private Product product;
    private int predictedQuantity;
    private int stockQuantity;
    private ForecastConfidence confidence;
    private int sampleSize;
    private LocalDateTime calculatedAt;
    private LocalDateTime forecastUntil;

    /**
     * Sets the ID of the forecast.
     *
     * @param id the ID of the forecast
     * @return the current instance of {@link DemandForecastBuilder}
     */
    public DemandForecastBuilder id(Long id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the product the forecast is about.
     *
     * @param product the product the forecast is about
     * @return the current instance of {@link DemandForecastBuilder}
     */
    public DemandForecastBuilder product(Product product) {
        this.product = product;
        return this;
    }

    /**
     * Sets the units expected to be sold until the end of the business day.
     *
     * @param predictedQuantity the predicted quantity
     * @return the current instance of {@link DemandForecastBuilder}
     */
    public DemandForecastBuilder predictedQuantity(int predictedQuantity) {
        this.predictedQuantity = predictedQuantity;
        return this;
    }

    /**
     * Sets the stock quantity at the moment of calculation.
     *
     * @param stockQuantity the stock quantity
     * @return the current instance of {@link DemandForecastBuilder}
     */
    public DemandForecastBuilder stockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
        return this;
    }

    /**
     * Sets the confidence level of the forecast.
     *
     * @param confidence the confidence level
     * @return the current instance of {@link DemandForecastBuilder}
     */
    public DemandForecastBuilder confidence(ForecastConfidence confidence) {
        this.confidence = confidence;
        return this;
    }

    /**
     * Sets the number of past sales considered in the calculation.
     *
     * @param sampleSize the number of past sales considered
     * @return the current instance of {@link DemandForecastBuilder}
     */
    public DemandForecastBuilder sampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
        return this;
    }

    /**
     * Sets the moment the forecast was calculated.
     *
     * @param calculatedAt the moment of calculation
     * @return the current instance of {@link DemandForecastBuilder}
     */
    public DemandForecastBuilder calculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
        return this;
    }

    /**
     * Sets the end of the business day the forecast refers to.
     *
     * @param forecastUntil the end of the business day
     * @return the current instance of {@link DemandForecastBuilder}
     */
    public DemandForecastBuilder forecastUntil(LocalDateTime forecastUntil) {
        this.forecastUntil = forecastUntil;
        return this;
    }

    /**
     * Builds and returns an instance of {@link DemandForecast} with the set properties.
     *
     * @return a new instance of {@link DemandForecast}
     */
    public DemandForecast build() {
        DemandForecast forecast = new DemandForecast();
        forecast.setId(id);
        forecast.setProduct(product);
        forecast.setPredictedQuantity(predictedQuantity);
        forecast.setStockQuantity(stockQuantity);
        forecast.setConfidence(confidence);
        forecast.setSampleSize(sampleSize);
        forecast.setCalculatedAt(calculatedAt);
        forecast.setForecastUntil(forecastUntil);
        return forecast;
    }
}
