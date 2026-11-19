package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.seudominio.perishrescue.business.validation.validators.OfferBusinessValidator;
import br.com.seudominio.perishrescue.business.validation.validators.OfferOrderBusinessValidator;
import br.com.seudominio.perishrescue.core.money.Money;
import br.com.seudominio.perishrescue.core.time.SystemTimeProvider;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.domain.dtos.ReserveOfferRequest;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.entities.Offer;
import br.com.seudominio.perishrescue.domain.entities.PerishableResource;
import br.com.seudominio.perishrescue.domain.enums.OfferStatus;
import br.com.seudominio.perishrescue.domain.mappers.OfferMapper;
import br.com.seudominio.perishrescue.domain.mappers.OfferOrderMapper;
import br.com.seudominio.perishrescue.domain.mappers.ResourceMapper;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OfferOrderRepository;
import br.com.seudominio.perishrescue.persistence.repositories.OfferRepository;
import br.com.seudominio.perishrescue.persistence.repositories.ResourceRepository;
import br.com.seudominio.perishrescue.domain.testsupport.TestResource;

import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Concurrency integration test for UC09: verifies that two simultaneous
 * reservations competing for the same {@link Offer} cannot oversell its
 * available quantity and that the losing transaction fails with
 * {@link ObjectOptimisticLockingFailureException}.
 */
@SpringBootTest(
        classes = OfferOrderConcurrencyTest.TestConfig.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:uc09_concurrency;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.open-in-view=false"
        })
class OfferOrderConcurrencyTest {

    private static final AtomicReference<CyclicBarrier> FIND_BARRIER = new AtomicReference<>();

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {Offer.class, TestResource.class})
    @EnableJpaRepositories(basePackageClasses = OfferRepository.class)
    @Import({
            OfferOrderService.class,
            OfferService.class,
            OfferOrderMapper.class,
            OfferMapper.class,
            ResourceMapper.class,
            OfferOrderBusinessValidator.class,
            OfferBusinessValidator.class,
            SystemTimeProvider.class
    })
    static class TestConfig {

        @Bean
        MessageUtils messageUtils() {
            return Mockito.mock(MessageUtils.class);
        }

        @Bean
        ResourceService resourceService() {
            return Mockito.mock(ResourceService.class);
        }

        @Bean
        static BeanPostProcessor offerRepositoryBarrierPostProcessor() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                    if (bean instanceof OfferRepository) {
                        ProxyFactory proxyFactory = new ProxyFactory(bean);
                        proxyFactory.setProxyTargetClass(false);
                        proxyFactory.addAdvice((MethodInterceptor) invocation -> {
                            Object result = invocation.proceed();
                            if ("findById".equals(invocation.getMethod().getName())) {
                                CyclicBarrier barrier = FIND_BARRIER.get();
                                if (barrier != null) {
                                    barrier.await(5, TimeUnit.SECONDS);
                                }
                            }
                            return result;
                        });
                        return proxyFactory.getProxy();
                    }
                    return bean;
                }
            };
        }
    }

    @Autowired
    private OffererRepository offererRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private OfferOrderRepository offerOrderRepository;

    @Autowired
    private OfferOrderService offerOrderService;

    @Test
    void twoSimultaneousReservationsDoNotExceedAvailableQuantityAndRaiseOptimisticLockConflict() throws Exception {
        Offerer offerer = offererRepository.save(Offerer.builder()
                .name("Padaria Central")
                .cnpj("12345678000199")
                .address("Rua A, 100")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@central.com")
                .passwordHash("hash")
                .build());

        PerishableResource resource = resourceRepository.save(TestResource.builder()
                .offerer(offerer)
                .name("Cesta de paes")
                .category("PADARIA")
                .originalPrice(Money.of(20.00))
                .currentPrice(Money.of(10.00))
                .stockQuantity(10)
                .build());

        Beneficiary firstBeneficiary = beneficiaryRepository.save(Beneficiary.builder()
                .name("Alice")
                .document("52998224725")
                .email("alice@example.com")
                .passwordHash("hash")
                .build());

        Beneficiary secondBeneficiary = beneficiaryRepository.save(Beneficiary.builder()
                .name("Bruno")
                .document("11144477735")
                .email("bruno@example.com")
                .passwordHash("hash")
                .build());

        Offer offer = offerRepository.save(Offer.builder()
                .resource(resource)
                .discountedPrice(Money.of(10.00))
                .availableQuantity(2)
                .expiresAt(LocalDateTime.now().plusHours(3))
                .status(OfferStatus.ACTIVE)
                .build());

        FIND_BARRIER.set(new CyclicBarrier(2));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable< Object>> tasks = List.of(
                    () -> offerOrderService.reserve(new ReserveOfferRequest(offer.getId(), 2), firstBeneficiary.getId()),
                    () -> offerOrderService.reserve(new ReserveOfferRequest(offer.getId(), 2), secondBeneficiary.getId())
            );

            List<Future<Object>> futures = executor.invokeAll(tasks);

            int successes = 0;
            List<Throwable> failures = new ArrayList<>();

            for (Future<Object> future : futures) {
                try {
                    future.get();
                    successes++;
                } catch (ExecutionException ex) {
                    failures.add(ex.getCause());
                }
            }

            assertThat(successes).isEqualTo(1);
            assertThat(failures).hasSize(1);
            assertThat(failures.get(0)).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        } finally {
            FIND_BARRIER.set(null);
            executor.shutdownNow();
        }

        Offer reloadedOffer = offerRepository.findById(offer.getId()).orElseThrow();
        assertThat(reloadedOffer.getAvailableQuantity()).isZero();
        assertThat(reloadedOffer.getStatus()).isEqualTo(OfferStatus.CLOSED);
        assertThat(offerOrderRepository.count()).isEqualTo(1L);
    }
}
