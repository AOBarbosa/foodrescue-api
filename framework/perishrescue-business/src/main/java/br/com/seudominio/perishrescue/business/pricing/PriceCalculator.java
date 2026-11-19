package br.com.seudominio.perishrescue.business.pricing;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The frozen pricing rules of the kernel, around the instance's
 * {@link PricingPolicy} (hot spot 2).
 *
 * <ul>
 *   <li>The policy's discount is capped at {@code perishrescue.pricing.max-discount}.</li>
 *   <li>A price comes from the larger discount between the policy's and the
 *       one already reflected in the asking price (the resource's current
 *       price, which carries a discount the offerer accepted from a
 *       recommendation, or a price the offerer set by hand).</li>
 *   <li>Repricing never raises a price: liquidation only goes down.</li>
 *   <li>A reservation freezes the unit price at reservation time, priced for
 *       that beneficiary.</li>
 * </ul>
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@Component
public class PriceCalculator {

    private final PricingPolicy pricingPolicy;
    private final BigDecimal maxDiscount;

    /**
     * Constructor.
     *
     * @param pricingPolicy the instance's (or the framework's default) pricing policy
     * @param maxDiscount   the largest discount the policy may grant, in percent
     */
    public PriceCalculator(PricingPolicy pricingPolicy,
                           @Value("${perishrescue.pricing.max-discount:90}") double maxDiscount) {
        this.pricingPolicy = pricingPolicy;
        this.maxDiscount = Percentage.of(maxDiscount).value();
    }

    /**
     * The price of an offer: the asking price, lowered by the policy if it
     * grants a bigger discount.
     *
     * @param resource    the offered resource
     * @param askingPrice the price the offer would otherwise have
     * @param now         the current date and time
     * @return the offer price, never above {@code askingPrice}
     */
    public Money offerPrice(PerishableResource resource, Money askingPrice, LocalDateTime now) {
        return lowest(askingPrice, policyPrice(new PricingContext(resource, null, now)));
    }

    /**
     * The unit price a beneficiary pays when reserving now.
     *
     * @param offer       the offer being reserved
     * @param beneficiary the beneficiary
     * @param now         the current date and time
     * @return the unit price, never above the offer's current price
     */
    public Money unitPriceFor(Offer offer, Beneficiary beneficiary, LocalDateTime now) {
        return lowest(offer.getDiscountedPrice(), policyPrice(new PricingContext(offer.getResource(), beneficiary, now)));
    }

    private Money policyPrice(PricingContext context) {
        Percentage discount = pricingPolicy.discountFor(context);
        BigDecimal capped = discount == null ? BigDecimal.ZERO : discount.value().min(maxDiscount);
        return context.resource().getOriginalPrice().applyDiscount(Percentage.of(capped));
    }

    private static Money lowest(Money a, Money b) {
        if (a == null) {
            return b;
        }
        return b.isGreaterThan(a) ? a : b;
    }
}
