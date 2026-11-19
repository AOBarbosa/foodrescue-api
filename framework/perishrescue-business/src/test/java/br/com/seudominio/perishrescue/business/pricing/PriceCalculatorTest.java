package br.com.seudominio.perishrescue.business.pricing;

import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.percentage.Percentage;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCalculatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 18, 0);

    private final TestResource resource = TestResource.builder().originalPrice(Money.of(10.00)).build();

    @Test
    void appliesThePolicyDiscountToTheOriginalPriceWhenItIsBigger() {
        PriceCalculator calculator = new PriceCalculator(context -> Percentage.of(30), 90);

        assertThat(calculator.offerPrice(resource, Money.of(10.00), NOW).amount()).isEqualByComparingTo("7.00");
    }

    @Test
    void keepsTheAskingPriceWhenItAlreadyCarriesABiggerDiscount() {
        PriceCalculator calculator = new PriceCalculator(context -> Percentage.of(30), 90);

        // e.g. the offerer accepted a 50% recommendation: asking price 5.00
        assertThat(calculator.offerPrice(resource, Money.of(5.00), NOW).amount()).isEqualByComparingTo("5.00");
    }

    @Test
    void capsThePolicyDiscountAtTheConfiguredMaximum() {
        PriceCalculator calculator = new PriceCalculator(context -> Percentage.of(100), 80);

        assertThat(calculator.offerPrice(resource, Money.of(10.00), NOW).amount()).isEqualByComparingTo("2.00");
    }

    @Test
    void pricesAReservationForTheBeneficiaryButNeverAboveTheOfferPrice() {
        Beneficiary charity = new Beneficiary();
        charity.setId(7L);
        PricingPolicy charityGetsMore = context -> context.optionalBeneficiary()
                .filter(beneficiary -> Long.valueOf(7L).equals(beneficiary.getId()))
                .map(beneficiary -> Percentage.of(60))
                .orElse(Percentage.of(0));
        PriceCalculator calculator = new PriceCalculator(charityGetsMore, 90);
        Offer offer = Offer.builder().resource(resource).discountedPrice(Money.of(8.00)).build();

        assertThat(calculator.unitPriceFor(offer, charity, NOW).amount()).isEqualByComparingTo("4.00");
        assertThat(calculator.unitPriceFor(offer, new Beneficiary(), NOW).amount()).isEqualByComparingTo("8.00");
    }
}
