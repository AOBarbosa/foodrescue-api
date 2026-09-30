package br.com.seudominio.foodrescue.rest.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.AiRecommendationService;
import br.com.seudominio.foodrescue.domain.dtos.DiscountRecommendationDTO;
import br.com.seudominio.foodrescue.domain.dtos.RespondToDiscountRecommendationRequest;
import br.com.seudominio.foodrescue.domain.enums.RecommendationDecision;
import br.com.seudominio.foodrescue.domain.enums.RecommendationStatus;
import br.com.seudominio.foodrescue.domain.enums.RecommendationType;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;
import br.com.seudominio.foodrescue.rest.security.AuthenticatedPrincipal;
import br.com.seudominio.foodrescue.rest.security.UserRole;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

class DiscountRecommendationControllerTest {

    private AiRecommendationService aiRecommendationService;
    private DiscountRecommendationController controller;
    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        aiRecommendationService = mock(AiRecommendationService.class);
        controller = new DiscountRecommendationController(aiRecommendationService);
        principal = new AuthenticatedPrincipal(1L, UserRole.ESTABLISHMENT);
    }

    private DiscountRecommendationDTO withStatus(RecommendationStatus status) {
        return new DiscountRecommendationDTO(
                500L, 10L, "Pao frances", RecommendationType.DISCOUNT, new BigDecimal("40.00"), status,
                new BigDecimal("10.00"), new BigDecimal("6.00"), new BigDecimal("6.00"),
                LocalDateTime.of(2026, 9, 14, 15, 0), LocalDateTime.of(2026, 9, 15, 15, 0), null);
    }

    @Test
    void recommendUsesAuthenticatedEstablishment() {
        DiscountRecommendationDTO recommendation = withStatus(RecommendationStatus.PENDING);
        when(aiRecommendationService.recommendDiscount(10L, 1L)).thenReturn(recommendation);

        ResponseEntity<ApiResponse<DiscountRecommendationDTO>> response = controller.recommend(10L, principal);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(recommendation);
        assertThat(response.getBody().success()).isTrue();
        verify(aiRecommendationService).recommendDiscount(10L, 1L);
    }

    @Test
    void historyUsesAuthenticatedEstablishment() {
        List<DiscountRecommendationDTO> history = List.of(withStatus(RecommendationStatus.ACCEPTED));
        when(aiRecommendationService.findHistoryForProduct(10L, 1L)).thenReturn(history);

        ResponseEntity<ApiResponse<List<DiscountRecommendationDTO>>> response = controller.history(10L, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(history);
        verify(aiRecommendationService).findHistoryForProduct(10L, 1L);
    }

    @Test
    void pendingUsesAuthenticatedEstablishment() {
        List<DiscountRecommendationDTO> pending = List.of(withStatus(RecommendationStatus.PENDING));
        when(aiRecommendationService.findPendingForEstablishment(1L)).thenReturn(pending);

        ResponseEntity<ApiResponse<List<DiscountRecommendationDTO>>> response = controller.pending(principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().data()).isSameAs(pending);
        verify(aiRecommendationService).findPendingForEstablishment(1L);
    }

    @Test
    void acceptedAnswerReportsThePriceWasUpdated() {
        RespondToDiscountRecommendationRequest request =
                new RespondToDiscountRecommendationRequest(RecommendationDecision.ACCEPT, null);
        when(aiRecommendationService.respondToRecommendation(500L, 1L, request))
                .thenReturn(withStatus(RecommendationStatus.ACCEPTED));

        ResponseEntity<ApiResponse<DiscountRecommendationDTO>> response =
                controller.respond(500L, request, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("price was updated");
        verify(aiRecommendationService).respondToRecommendation(500L, 1L, request);
    }

    @Test
    void refusedAnswerReportsThePriceWasNotChanged() {
        RespondToDiscountRecommendationRequest request =
                new RespondToDiscountRecommendationRequest(RecommendationDecision.REFUSE, null);
        when(aiRecommendationService.respondToRecommendation(500L, 1L, request))
                .thenReturn(withStatus(RecommendationStatus.REFUSED));

        ResponseEntity<ApiResponse<DiscountRecommendationDTO>> response =
                controller.respond(500L, request, principal);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("was not changed");
    }
}
