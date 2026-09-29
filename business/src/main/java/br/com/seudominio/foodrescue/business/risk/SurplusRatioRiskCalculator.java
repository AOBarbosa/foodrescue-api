package br.com.seudominio.foodrescue.business.risk;

import br.com.seudominio.foodrescue.core.percentage.Percentage;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Initial {@link RiskCalculator}: the risk is the share of the current stock
 * that the forecast does not absorb, i.e.
 * {@code (stock - predictedDemand) / stock * 100}.
 *
 * <p>A product whose predicted demand covers (or exceeds) its stock has no
 * expected surplus and therefore {@code 0%} risk. An empty stock is also
 * {@code 0%}: there is nothing left to waste.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class SurplusRatioRiskCalculator implements RiskCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int SCALE = 2;

    @Override
    public Percentage calculate(int stockQuantity, int predictedQuantity) {
        int surplus = expectedSurplus(stockQuantity, predictedQuantity);
        if (surplus == 0 || stockQuantity <= 0) {
            return Percentage.of(BigDecimal.ZERO);
        }

        BigDecimal ratio = BigDecimal.valueOf(surplus)
                .divide(BigDecimal.valueOf(stockQuantity), SCALE + 2, RoundingMode.HALF_UP)
                .multiply(HUNDRED);

        return Percentage.of(ratio.setScale(SCALE, RoundingMode.HALF_UP));
    }
}
