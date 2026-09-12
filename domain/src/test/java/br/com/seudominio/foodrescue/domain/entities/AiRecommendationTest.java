package br.com.seudominio.foodrescue.domain.entities;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.percentage.Percentage;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;

import org.junit.jupiter.api.Test;

class AiRecommendationTest {

    private Product anyProduct() {
        Establishment establishment = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj("12345678000199")
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hashed-password")
                .build();

        return Product.builder()
                .establishment(establishment)
                .name("Pão francês")
                .category("bakery")
                .originalPrice(Money.of(5.0))
                .stockQuantity(10)
                .build();
    }

    @Test
    void defaultsStatusToPending() {
        AiRecommendation recommendation = AiRecommendation.builder()
                .product(anyProduct())
                .type(RecommendationType.DISCOUNT)
                .suggestedPercentage(Percentage.of(20.0))
                .build();

        assertThat(recommendation.getStatus()).isEqualTo(RecommendationStatus.PENDING);
        assertThat(recommendation.getPreviousRecommendationId()).isNull();
    }

    @Test
    void buildsRecommendationWithAllFields() {
        AiRecommendation recommendation = AiRecommendation.builder()
                .product(anyProduct())
                .type(RecommendationType.SURPLUS_DESTINATION)
                .previousRecommendationId(42L)
                .build();

        assertThat(recommendation.getType()).isEqualTo(RecommendationType.SURPLUS_DESTINATION);
        assertThat(recommendation.getPreviousRecommendationId()).isEqualTo(42L);
    }
}
