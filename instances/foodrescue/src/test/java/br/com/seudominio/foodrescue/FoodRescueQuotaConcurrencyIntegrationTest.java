package br.com.seudominio.foodrescue;

import br.com.seudominio.foodrescue.domain.FoodItem;
import br.com.seudominio.foodrescue.domain.StorageTemperature;
import br.com.seudominio.perishrescue.business.services.ReservationService;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OfferRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ResourceRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The daily quota per CPF (FoodRescue: 3 units) must hold even when the same
 * beneficiary sends several reservations at the same time: the kernel locks
 * the beneficiary row, so the second reservation waits and sees the first.
 *
 * @author Clovis Medeiros
 * @since 1.0.0
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:quotaconcurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false"
})
class FoodRescueQuotaConcurrencyIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private OffererRepository offererRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private OfferRepository offerRepository;

    private Offer offerOf(Offerer offerer, String name) {
        FoodItem item = FoodItem.builder()
                .offerer(offerer)
                .name(name)
                .category("salgado")
                .originalPrice(Money.of(10.00))
                .stockQuantity(10)
                .build();
        item.setStorageTemperature(StorageTemperature.AMBIENT);
        FoodItem saved = resourceRepository.save(item);
        return offerRepository.save(Offer.builder()
                .resource(saved)
                .discountedPrice(Money.of(10.00))
                .availableQuantity(10)
                .expiresAt(LocalDateTime.now().plusHours(3))
                .build());
    }

    @Test
    void twoSimultaneousReservationsOfTheSameCpfCannotTogetherExceedTheDailyQuota() throws Exception {
        Offerer offerer = offererRepository.save(Offerer.builder()
                .name("Padaria 24h")
                .cnpj(String.valueOf(System.nanoTime()))
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.MIN, LocalTime.of(23, 59, 59))
                .email("padaria" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .build());
        Beneficiary beneficiary = beneficiaryRepository.save(Beneficiary.builder()
                .name("Maria")
                .document(String.valueOf(System.nanoTime()).substring(0, 11))
                .email("maria" + System.nanoTime() + "@example.com")
                .passwordHash("hash")
                .build());
        // two different offers, so the offer's own optimistic lock plays no part
        Offer first = offerOf(offerer, "Pao de queijo");
        Offer second = offerOf(offerer, "Coxinha");

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (Offer offer : List.of(first, second)) {
                Callable<Boolean> reserveTwo = () -> {
                    start.await();
                    try {
                        reservationService.reserve(offer.getId(), beneficiary.getId(), 2);
                        return true;
                    } catch (ValidationException quotaExceeded) {
                        return false;
                    }
                };
                results.add(executor.submit(reserveTwo));
            }
            start.countDown();

            int succeeded = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) {
                    succeeded++;
                }
            }
            assertThat(succeeded).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}
