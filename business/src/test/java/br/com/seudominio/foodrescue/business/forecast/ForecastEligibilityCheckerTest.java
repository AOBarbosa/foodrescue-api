package br.com.seudominio.foodrescue.business.forecast;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.domain.entities.Sale;

import org.junit.jupiter.api.Test;

import java.util.Collections;

class ForecastEligibilityCheckerTest {

    private final ForecastEligibilityChecker checker = new ForecastEligibilityChecker(5);

    @Test
    void isEligibleWithMinimumNumberOfSales() {
        assertThat(checker.isEligible(Collections.nCopies(5, new Sale()))).isTrue();
    }

    @Test
    void isNotEligibleBelowMinimumNumberOfSales() {
        assertThat(checker.isEligible(Collections.nCopies(4, new Sale()))).isFalse();
    }
}
