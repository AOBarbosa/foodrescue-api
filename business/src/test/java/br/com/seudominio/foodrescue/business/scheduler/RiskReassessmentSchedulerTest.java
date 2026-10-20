package br.com.seudominio.foodrescue.business.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.seudominio.foodrescue.business.services.AiRecommendationService;

import org.junit.jupiter.api.Test;

class RiskReassessmentSchedulerTest {

    @Test
    void onlyDelegatesToTheService() {
        AiRecommendationService service = mock(AiRecommendationService.class);

        new RiskReassessmentScheduler(service).reassess();

        verify(service).reassessSurplusDestination();
    }
}
