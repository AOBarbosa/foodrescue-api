package br.com.seudominio.foodrescue.business.risk;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

class SurplusRatioRiskCalculatorTest {

    private final SurplusRatioRiskCalculator calculator = new SurplusRatioRiskCalculator();

    @Test
    void riskIsTheShareOfStockTheForecastDoesNotAbsorb() {
        assertThat(calculator.calculate(20, 6)).isEqualTo(Percentage.of(70));
        assertThat(calculator.expectedSurplus(20, 6)).isEqualTo(14);
    }

    @Test
    void riskIsZeroWhenStockEqualsPredictedDemand() {
        assertThat(calculator.calculate(10, 10)).isEqualTo(Percentage.of(BigDecimal.ZERO));
        assertThat(calculator.expectedSurplus(10, 10)).isZero();
    }

    @Test
    void riskIsZeroWhenStockIsLowerThanPredictedDemand() {
        assertThat(calculator.calculate(4, 10)).isEqualTo(Percentage.of(BigDecimal.ZERO));
        assertThat(calculator.expectedSurplus(4, 10)).isZero();
    }

    @Test
    void riskIsFullWhenNothingIsExpectedToBeSold() {
        assertThat(calculator.calculate(15, 0)).isEqualTo(Percentage.of(100));
    }

    @Test
    void riskIsZeroWhenThereIsNoStockLeftToWaste() {
        assertThat(calculator.calculate(0, 0)).isEqualTo(Percentage.of(BigDecimal.ZERO));
    }

    @Test
    void riskIsRoundedToTwoDecimalPlaces() {
        assertThat(calculator.calculate(3, 1).value()).isEqualByComparingTo("66.67");
    }
}
