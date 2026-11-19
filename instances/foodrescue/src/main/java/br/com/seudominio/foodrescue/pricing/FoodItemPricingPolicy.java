package br.com.seudominio.foodrescue.pricing;

import br.com.seudominio.foodrescue.domain.FoodItem;
import br.com.seudominio.perishrescue.business.pricing.PricingContext;
import br.com.seudominio.perishrescue.business.pricing.PricingPolicy;
import br.com.seudominio.perishrescue.business.pricing.StepwiseDepreciationPolicy;
import br.com.seudominio.perishrescue.business.pricing.StepwiseDepreciationPolicy.TimeReference;
import br.com.seudominio.perishrescue.core.percentage.Percentage;

import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * FoodRescue's implementation of <b>hot spot 2</b> (pricing and liquidation):
 *
 * <ul>
 *   <li>surprise bags always get a fixed 75% off;</li>
 *   <li>other items get a regressive discount by the hours left until the
 *       offerer closes: 30% off from 4 hours before closing, 70% off from
 *       1 hour before.</li>
 * </ul>
 *
 * <p>Reuses the framework's {@link StepwiseDepreciationPolicy} for the
 * regressive part and adds the surprise-bag rule on top (Decorator). Being a
 * {@code PricingPolicy} bean, it replaces the framework's default.</p>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class FoodItemPricingPolicy implements PricingPolicy {

    static final Percentage SURPRISE_BAG_DISCOUNT = Percentage.of(75);

    private final PricingPolicy hoursUntilClosing = new StepwiseDepreciationPolicy(
            TimeReference.UNTIL_DEADLINE,
            ChronoUnit.HOURS,
            Map.of(4L, Percentage.of(30), 1L, Percentage.of(70)));

    @Override
    public Percentage discountFor(PricingContext context) {
        if (context.resource() instanceof FoodItem item && item.isSurpriseBag()) {
            return SURPRISE_BAG_DISCOUNT;
        }
        return hoursUntilClosing.discountFor(context);
    }
}
