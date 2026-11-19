package br.com.seudominio.foodrescue;

import br.com.seudominio.foodrescue.pricing.FoodItemPricingPolicy;
import br.com.seudominio.foodrescue.rest.FoodItemController;
import br.com.seudominio.perishrescue.business.pricing.PricingPolicy;
import br.com.seudominio.perishrescue.business.rescue.PickupWindowRule;
import br.com.seudominio.perishrescue.business.rescue.QuotaPerBeneficiaryRule;
import br.com.seudominio.perishrescue.business.rescue.RescueRule;
import br.com.seudominio.perishrescue.business.services.ReservationService;
import br.com.seudominio.perishrescue.business.services.OfferService;
import br.com.seudominio.perishrescue.persistence.repositories.OfferRepository;
import br.com.seudominio.perishrescue.rest.controller.OfferController;
import br.com.seudominio.perishrescue.rest.security.JwtTokenProvider;

import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the FoodRescue instance with the whole framework kernel plugged in
 * through {@link br.com.seudominio.perishrescue.rest.boot.EnablePerishRescue},
 * against an in-memory H2 database instead of PostgreSQL.
 *
 * <p>Guards the wiring between instance and framework: if the framework's
 * components, entities or repositories stop being found from the instance's
 * package, the context fails to start and this test fails.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:foodrescue;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false"
})
class FoodRescueApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void bootsWithTheFrameworkKernelPluggedIn() {
        assertThat(context.getBeansOfType(OfferService.class)).hasSize(1);
        assertThat(context.getBeansOfType(ReservationService.class)).hasSize(1);
        assertThat(context.getBeansOfType(OfferRepository.class)).hasSize(1);
        assertThat(context.getBeansOfType(OfferController.class)).hasSize(1);
        assertThat(context.getBeansOfType(FoodItemController.class)).hasSize(1);
        assertThat(context.getBeansOfType(JwtTokenProvider.class)).hasSize(1);
    }

    @Test
    void theInstanceHotSpotsReplaceTheFrameworkDefaults() {
        assertThat(context.getBean(PricingPolicy.class)).isInstanceOf(FoodItemPricingPolicy.class);
        assertThat(context.getBean(br.com.seudominio.perishrescue.business.forecast.ForecastHorizon.class))
                .isInstanceOf(br.com.seudominio.perishrescue.business.forecast.DeadlineForecastHorizon.class);
        assertThat(context.getBeansOfType(RescueRule.class).values())
                .hasSize(2)
                .hasAtLeastOneElementOfType(QuotaPerBeneficiaryRule.class)
                .hasAtLeastOneElementOfType(PickupWindowRule.class);
    }

    @Test
    void mapsTheFrameworkEntities() {
        assertThat(entityManagerFactory.getMetamodel().getEntities())
                .extracting(entity -> entity.getJavaType().getSimpleName())
                .contains("Offerer", "Beneficiary", "PerishableResource", "FoodItem", "Offer", "Reservation",
                        "Sale", "DemandForecast", "AiRecommendation");
    }
}
