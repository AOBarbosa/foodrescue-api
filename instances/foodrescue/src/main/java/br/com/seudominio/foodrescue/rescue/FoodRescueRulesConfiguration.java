package br.com.seudominio.foodrescue.rescue;

import br.com.seudominio.perishrescue.business.rescue.PickupWindowRule;
import br.com.seudominio.perishrescue.business.rescue.QuotaPerBeneficiaryRule;
import br.com.seudominio.perishrescue.business.rescue.RescueRule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FoodRescue's implementation of <b>hot spot 3</b> (rescue restrictions),
 * built from the framework's reusable rules:
 *
 * <ul>
 *   <li>at most 3 units (items or surprise bags) per CPF per day;</li>
 *   <li>in-person pickup only within the offerer's business hours.</li>
 * </ul>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
public class FoodRescueRulesConfiguration {

    /**
     * Daily quota per CPF.
     */
    static final int DAILY_UNITS_PER_CPF = 3;

    /**
     * At most {@value #DAILY_UNITS_PER_CPF} units per CPF per day.
     *
     * @return the quota rule
     */
    @Bean
    public RescueRule dailyQuotaPerCpf() {
        return new QuotaPerBeneficiaryRule(DAILY_UNITS_PER_CPF);
    }

    /**
     * Pickup within business hours.
     *
     * @return the pickup window rule
     */
    @Bean
    public RescueRule pickupWithinBusinessHours() {
        return new PickupWindowRule();
    }
}
