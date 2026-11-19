package br.com.seudominio.perishrescue.business.autoconfigure;

import br.com.seudominio.perishrescue.business.forecast.DeadlineForecastHorizon;
import br.com.seudominio.perishrescue.business.forecast.ForecastHorizon;
import br.com.seudominio.perishrescue.business.pricing.PricingPolicy;
import br.com.seudominio.perishrescue.business.pricing.StepwiseDepreciationPolicy;
import br.com.seudominio.perishrescue.business.pricing.StepwiseDepreciationPolicy.TimeReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import java.time.temporal.ChronoUnit;

/**
 * Default implementations of the framework's hot spots, used when the
 * instance declares none of its own.
 *
 * <p>Registered as a Spring Boot auto-configuration (see
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports})
 * rather than component-scanned, so it is processed <em>after</em> the
 * instance's beans and {@link ConditionalOnMissingBean} reliably backs off
 * when the instance provides its own implementation.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@AutoConfiguration
public class PerishRescueDefaultsAutoConfiguration {

    /**
     * Hot spot 2 default: a {@link StepwiseDepreciationPolicy} configured by
     * {@code perishrescue.pricing.reference}, {@code .unit} and {@code .steps}
     * (e.g. {@code UNTIL_DEADLINE}, {@code HOURS}, {@code 4:30,1:70}). With no
     * steps it grants no discount.
     *
     * @param reference where the steps are measured from
     * @param unit      the unit of the step thresholds
     * @param steps     the steps as {@code threshold:percent} pairs
     * @return the default pricing policy
     */
    @Bean
    @ConditionalOnMissingBean(PricingPolicy.class)
    public PricingPolicy stepwiseDepreciationPolicy(
            @Value("${perishrescue.pricing.reference:UNTIL_DEADLINE}") TimeReference reference,
            @Value("${perishrescue.pricing.unit:HOURS}") ChronoUnit unit,
            @Value("${perishrescue.pricing.steps:}") String steps) {
        return new StepwiseDepreciationPolicy(reference, unit, StepwiseDepreciationPolicy.parseSteps(steps));
    }

    /**
     * Hot spot 4 default: forecast until the resource's critical deadline,
     * counting time in {@code perishrescue.forecast.granularity}.
     *
     * @param granularity the unit the remaining time is counted in
     * @return the default forecast horizon
     */
    @Bean
    @ConditionalOnMissingBean(ForecastHorizon.class)
    public ForecastHorizon deadlineForecastHorizon(
            @Value("${perishrescue.forecast.granularity:HOURS}") ChronoUnit granularity) {
        return new DeadlineForecastHorizon(granularity);
    }
}
