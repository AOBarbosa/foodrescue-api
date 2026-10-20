package br.com.seudominio.foodrescue.business.scheduler;

import br.com.seudominio.foodrescue.business.services.AiRecommendationService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically triggers the reassessment of products still at risk after a
 * discount (UC10). It holds no business logic: it only invokes the service.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class RiskReassessmentScheduler {

    private final AiRecommendationService aiRecommendationService;

    /**
     * Constructor.
     *
     * @param aiRecommendationService the AI recommendation service
     */
    public RiskReassessmentScheduler(AiRecommendationService aiRecommendationService) {
        this.aiRecommendationService = aiRecommendationService;
    }

    /**
     * Runs one reassessment cycle.
     */
    @Scheduled(fixedDelayString = "${app.surplus.reassessment-interval-ms:900000}")
    public void reassess() {
        aiRecommendationService.reassessSurplusDestination();
    }
}
