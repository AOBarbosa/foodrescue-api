package br.com.seudominio.foodrescue.business.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

import org.junit.jupiter.api.Test;

class RiskWeightedDiscountStrategyTest {

    private static final double RISK_FACTOR = 0.6;
    private static final double URGENCY_BONUS = 15;
    private static final long URGENT_HOURS = 4;
    private static final double MAX_DISCOUNT = 60;

    private final RiskWeightedDiscountStrategy strategy =
            new RiskWeightedDiscountStrategy(RISK_FACTOR, URGENCY_BONUS, URGENT_HOURS, MAX_DISCOUNT);

    private DiscountContext context(double risk, int stock, int surplus, long hoursUntilClosing, Long daysToExpire) {
        return new DiscountContext(Percentage.of(risk), stock, surplus, hoursUntilClosing, daysToExpire);
    }

    @Test
    void suggestsAFractionOfTheRiskWhenThereIsStillTime() {
        assertThat(strategy.suggest(context(80, 20, 16, 8, null)).value()).isEqualByComparingTo("48.00");
    }

    @Test
    void addsTheUrgencyBonusCloseToClosingTime() {
        assertThat(strategy.suggest(context(50, 20, 10, 2, null)).value()).isEqualByComparingTo("45.00");
    }

    @Test
    void capsTheSuggestionWhenTheUrgencyBonusWouldPushItOverTheMaximum() {
        // 80 * 0.6 = 48, + 15 de urgencia = 63, limitado a 60
        assertThat(strategy.suggest(context(80, 20, 16, 2, null)).value()).isEqualByComparingTo("60.00");
    }

    @Test
    void addsTheUrgencyBonusWhenTheProductExpiresTomorrow() {
        assertThat(strategy.suggest(context(50, 20, 10, 10, 1L)).value()).isEqualByComparingTo("45.00");
    }

    @Test
    void ignoresAnExpirationDateThatIsStillFarAway() {
        assertThat(strategy.suggest(context(50, 20, 10, 10, 30L)).value()).isEqualByComparingTo("30.00");
    }

    @Test
    void neverSuggestsMoreThanTheConfiguredMaximum() {
        assertThat(strategy.suggest(context(100, 20, 20, 1, 0L)).value()).isEqualByComparingTo("60.00");
        assertThat(strategy.getMaxDiscount()).isEqualTo(Percentage.of(60));
    }

    @Test
    void suggestsNothingWhenNoSurplusIsExpected() {
        assertThat(strategy.suggest(context(0, 10, 0, 1, 0L)).value()).isEqualByComparingTo("0.00");
    }

    @Test
    void treatsATimeAfterClosingAsUrgent() {
        assertThat(strategy.suggest(context(50, 20, 10, -1, null)).value()).isEqualByComparingTo("45.00");
    }
}
