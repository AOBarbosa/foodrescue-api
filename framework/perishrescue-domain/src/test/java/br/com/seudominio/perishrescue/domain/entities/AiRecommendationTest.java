package br.com.seudominio.perishrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.perishrescue.domain.enums.RecommendationType;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

class AiRecommendationTest {

    private PerishableResource anyResource() {
        Offerer offerer = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();

        return TestResource.builder()
                .offerer(offerer)
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .stockQuantity(10)
                .build();
    }

    @Test
    void defaultsStatusToPending() {
        AiRecommendation recommendation = AiRecommendation.builder()
                .resource(anyResource())
                .type(RecommendationType.DISCOUNT)
                .suggestedPercentage(Percentage.of(20.0))
                .build();

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.PENDING);
        assertThat(recommendation.getPreviousRecommendationId()).isNull();
    }

    @Test
    void buildsRecommendationWithAllFields() {
        AiRecommendation recommendation = AiRecommendation.builder()
                .resource(anyResource())
                .type(RecommendationType.SURPLUS_DESTINATION)
                .previousRecommendationId(42L)
                .build();

        assertThat(recommendation.getType()).isEqualTo(RecommendationType.SURPLUS_DESTINATION);
        assertThat(recommendation.getPreviousRecommendationId()).isEqualTo(42L);
    }
}
