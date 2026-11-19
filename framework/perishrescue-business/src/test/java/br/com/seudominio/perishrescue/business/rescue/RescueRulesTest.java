package br.com.seudominio.perishrescue.business.rescue;

import br.com.seudominio.perishrescue.core.validation.ValidationError;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RescueRulesTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 18, 15, 0);

    private final Beneficiary beneficiary = beneficiary();
    private final Offer offer = offerOfOffererOpenFrom(LocalTime.of(7, 0), LocalTime.of(20, 0));

    private static Beneficiary beneficiary() {
        Beneficiary b = new Beneficiary();
        b.setId(50L);
        return b;
    }

    private static Offer offerOfOffererOpenFrom(LocalTime opening, LocalTime closing) {
        Offerer offerer = Offerer.builder().businessHours(opening, closing).build();
        Offer offer = Offer.builder().resource(TestResource.builder().offerer(offerer).build()).build();
        offer.setId(700L);
        return offer;
    }

    private RescueContext context(int quantity, LocalDateTime now, int alreadyReservedToday) {
        return new RescueContext(offer, beneficiary, quantity, now, Map.of(),
                (beneficiaryId, from, to) -> alreadyReservedToday);
    }

    @Test
    void quotaAllowsUpToTheDailyLimitPerCpf() {
        RescueRule quota = new QuotaPerBeneficiaryRule(3);

        assertThat(quota.check(context(2, NOW, 1))).isEmpty();
        Optional<ValidationError> refused = quota.check(context(2, NOW, 2));
        assertThat(refused).get().extracting(ValidationError::code).isEqualTo("RESCUE_QUOTA_EXCEEDED");
    }

    @Test
    void quotaCountsOnlyTheCurrentDay() {
        RescueRule quota = new QuotaPerBeneficiaryRule(3);
        LocalDateTime[] period = new LocalDateTime[2];
        RescueContext context = new RescueContext(offer, beneficiary, 1, NOW, Map.of(), (beneficiaryId, from, to) -> {
            period[0] = from;
            period[1] = to;
            return 0;
        });

        quota.check(context);

        assertThat(period[0]).isEqualTo(NOW.toLocalDate().atStartOfDay());
        assertThat(period[1]).isEqualTo(NOW.toLocalDate().plusDays(1).atStartOfDay());
    }

    @Test
    void pickupWindowOnlyAcceptsReservationsWhileTheOffererIsOpen() {
        RescueRule window = new PickupWindowRule();

        assertThat(window.check(context(1, NOW, 0))).isEmpty();
        assertThat(window.check(context(1, NOW.withHour(6), 0))).get()
                .extracting(ValidationError::code).isEqualTo("OUTSIDE_BUSINESS_HOURS");
        assertThat(window.check(context(1, NOW.withHour(20), 0))).isPresent();
    }

    @Test
    void minimumLotRefusesSmallerReservations() {
        RescueRule lot = new MinimumLotRule(5);

        assertThat(lot.check(context(5, NOW, 0))).isEmpty();
        assertThat(lot.check(context(4, NOW, 0))).get()
                .extracting(ValidationError::code).isEqualTo("BELOW_MINIMUM_LOT");
    }

    @Test
    void rulesRefuseNonPositiveParameters() {
        assertThatThrownBy(() -> new QuotaPerBeneficiaryRule(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MinimumLotRule(0)).isInstanceOf(IllegalArgumentException.class);
    }
}
