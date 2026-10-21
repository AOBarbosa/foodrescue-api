package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.validation.validators.OfferBusinessValidator;
import br.com.seudominio.foodrescue.core.money.Money;
import br.com.seudominio.foodrescue.core.time.TimeProvider;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.CreateOfferRequest;
import br.com.seudominio.foodrescue.domain.dtos.OfferDTO;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.entities.Offer;
import br.com.seudominio.foodrescue.domain.entities.Product;
import br.com.seudominio.foodrescue.domain.enums.OfferStatus;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.OfferMapper;
import br.com.seudominio.foodrescue.persistence.repositories.OfferRepository;

import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class OfferServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 14, 15, 0);
    private static final LocalDateTime CLOSING = LocalDateTime.of(2026, 9, 14, 22, 0);

    private OfferRepository offerRepository;
    private ProductService productService;
    private OfferService service;
    private Establishment establishment;
    private Product product;
    private long nextId;

    @BeforeEach
    void setUp() {
        offerRepository = mock(OfferRepository.class);
        productService = mock(ProductService.class);
        TimeProvider timeProvider = mock(TimeProvider.class);
        when(timeProvider.now()).thenReturn(NOW);

        nextId = 800L;
        when(offerRepository.save(any(Offer.class))).thenAnswer(invocation -> {
            Offer saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(nextId++);
                saved.setCreationDate(NOW);
            }
            return saved;
        });

        Validator beanValidator = mock(Validator.class);
        when(beanValidator.validate(any())).thenReturn(java.util.Collections.emptySet());

        service = new OfferService(
                offerRepository, new OfferMapper(), beanValidator, mock(MessageUtils.class),
                productService, new OfferBusinessValidator(beanValidator, timeProvider),
                timeProvider, "22:00");

        establishment = new Establishment();
        establishment.setId(1L);

        product = new Product();
        product.setId(10L);
        product.setName("Pao de queijo");
        product.setEstablishment(establishment);
        product.setOriginalPrice(Money.of(10.00));
        product.setCurrentPrice(Money.of(5.20));
        product.setStockQuantity(20);

        when(productService.findProductOwnedBy(10L, 1L)).thenReturn(product);
        when(offerRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, OfferStatus.ACTIVE)).thenReturn(Optional.empty());
    }

    private Offer activeOffer(Long id, int quantity) {
        Offer offer = Offer.builder()
                .id(id)
                .product(product)
                .discountedPrice(Money.of(5.20))
                .availableQuantity(quantity)
                .expiresAt(CLOSING)
                .build();
        offer.setCreationDate(NOW);
        return offer;
    }

    // ---------- criacao manual ----------

    @Test
    void createOfferFillsEveryFieldFromTheProductWhenOnlyTheIdIsGiven() {
        OfferDTO dto = service.createOffer(new CreateOfferRequest(10L, null, null, null), 1L);

        assertThat(dto.id()).isEqualTo(800L);
        assertThat(dto.productId()).isEqualTo(10L);
        assertThat(dto.productName()).isEqualTo("Pao de queijo");
        assertThat(dto.originalPrice()).isEqualByComparingTo("10.00");
        assertThat(dto.discountedPrice()).isEqualByComparingTo("5.20");
        assertThat(dto.availableQuantity()).isEqualTo(20);
        assertThat(dto.expiresAt()).isEqualTo(CLOSING);
        assertThat(dto.status()).isEqualTo(OfferStatus.ACTIVE);
        assertThat(dto.priority()).isFalse();
    }

    @Test
    void createOfferHonoursTheInformedFields() {
        OfferDTO dto = service.createOffer(new CreateOfferRequest(
                10L, new BigDecimal("7.00"), 5, NOW.plusHours(2)), 1L);

        assertThat(dto.discountedPrice()).isEqualByComparingTo("7.00");
        assertThat(dto.availableQuantity()).isEqualTo(5);
        assertThat(dto.expiresAt()).isEqualTo(NOW.plusHours(2));
    }

    @Test
    void createOfferRejectsAProductWithoutStock() {
        product.setStockQuantity(0);

        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(10L, null, null, null), 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has no stock to offer");
        verify(offerRepository, never()).save(any(Offer.class));
    }

    @Test
    void createOfferRejectsAQuantityAboveTheStock() {
        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(10L, null, 50, null), 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("exceeds the product stock of 20");
    }

    @Test
    void createOfferRejectsASecondActiveOfferForTheSameProduct() {
        when(offerRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, OfferStatus.ACTIVE)).thenReturn(Optional.of(activeOffer(700L, 20)));

        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(10L, null, null, null), 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("already has an active offer");
    }

    @Test
    void createOfferRejectsAPriceAboveTheOriginal() {
        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(
                10L, new BigDecimal("15.00"), null, null), 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("must not be greater than the product's original price");
    }

    @Test
    void createOfferRejectsADeadlineInThePast() {
        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(
                10L, null, null, NOW.minusHours(1)), 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expiresAt must be in the future");
    }

    @Test
    void createOfferRejectsAProductFromAnotherEstablishment() {
        when(productService.findProductOwnedBy(10L, 2L)).thenThrow(new EntityNotFoundException(Product.class, 10L));

        assertThatThrownBy(() -> service.createOffer(new CreateOfferRequest(10L, null, null, null), 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- criacao automatica (UC07) ----------

    @Test
    void createFromAcceptedDiscountPublishesAnOfferAtTheDiscountedPrice() {
        Optional<OfferDTO> dto = service.createFromAcceptedDiscount(10L, 1L);

        assertThat(dto).isPresent();
        assertThat(dto.get().discountedPrice()).isEqualByComparingTo("5.20");
        assertThat(dto.get().originalPrice()).isEqualByComparingTo("10.00");
        assertThat(dto.get().availableQuantity()).isEqualTo(20);
        assertThat(dto.get().expiresAt()).isEqualTo(CLOSING);
        assertThat(dto.get().status()).isEqualTo(OfferStatus.ACTIVE);
    }

    @Test
    void createFromAcceptedDiscountSupersedesTheStaleOffer() {
        Offer stale = activeOffer(700L, 20);
        when(offerRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(
                10L, OfferStatus.ACTIVE)).thenReturn(Optional.of(stale));

        Optional<OfferDTO> dto = service.createFromAcceptedDiscount(10L, 1L);

        assertThat(stale.getStatus()).isEqualTo(OfferStatus.CLOSED);
        assertThat(dto).isPresent();
        assertThat(dto.get().id()).isEqualTo(800L);
        assertThat(dto.get().status()).isEqualTo(OfferStatus.ACTIVE);
    }

    @Test
    void createFromAcceptedDiscountPublishesNothingWhenThereIsNoStock() {
        product.setStockQuantity(0);

        assertThat(service.createFromAcceptedDiscount(10L, 1L)).isEmpty();
        verify(offerRepository, never()).save(any(Offer.class));
    }

    // ---------- encerramento ----------

    @Test
    void decreaseQuantityClosesTheOfferWhenNothingIsLeft() {
        Offer offer = activeOffer(700L, 3);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        Offer updated = service.decreaseQuantity(700L, 3);

        assertThat(updated.getAvailableQuantity()).isZero();
        assertThat(updated.getStatus()).isEqualTo(OfferStatus.CLOSED);
    }

    @Test
    void decreaseQuantityKeepsTheOfferActiveWhileUnitsRemain() {
        Offer offer = activeOffer(700L, 10);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        Offer updated = service.decreaseQuantity(700L, 4);

        assertThat(updated.getAvailableQuantity()).isEqualTo(6);
        assertThat(updated.getStatus()).isEqualTo(OfferStatus.ACTIVE);
    }

    @Test
    void decreaseQuantityRejectsMoreUnitsThanAvailable() {
        Offer offer = activeOffer(700L, 2);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.decreaseQuantity(700L, 5))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has only 2 units left");
        assertThat(offer.getAvailableQuantity()).isEqualTo(2);
    }

    @Test
    void decreaseQuantityRejectsAClosedOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setStatus(OfferStatus.CLOSED);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.decreaseQuantity(700L, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is CLOSED");
    }

    @Test
    void decreaseQuantityRejectsAnExpiredOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setExpiresAt(NOW.minusMinutes(1));
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.decreaseQuantity(700L, 1))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("has expired");
        assertThat(offer.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void decreaseQuantityRejectsANonExistentOffer() {
        when(offerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.decreaseQuantity(999L, 1))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void closeOffersForDepletedProductClosesEveryOpenOffer() {
        Offer first = activeOffer(700L, 5);
        Offer second = activeOffer(701L, 3);
        when(offerRepository.findByProductIdAndStatusAndActiveTrue(10L, OfferStatus.ACTIVE))
                .thenReturn(List.of(first, second));

        int closed = service.closeOffersForDepletedProduct(10L);

        assertThat(closed).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(OfferStatus.CLOSED);
        assertThat(second.getStatus()).isEqualTo(OfferStatus.CLOSED);
    }

    @Test
    void closeOffersForDepletedProductIsSafeWhenThereIsNothingOpen() {
        when(offerRepository.findByProductIdAndStatusAndActiveTrue(10L, OfferStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(service.closeOffersForDepletedProduct(10L)).isZero();
        verify(offerRepository, never()).save(any(Offer.class));
    }

    @Test
    void closeOfferByHandClosesAnActiveOffer() {
        Offer offer = activeOffer(700L, 5);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThat(service.closeOffer(700L, 1L).status()).isEqualTo(OfferStatus.CLOSED);
    }

    @Test
    void closeOfferRejectsAnAlreadyClosedOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setStatus(OfferStatus.CLOSED);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.closeOffer(700L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is CLOSED");
    }

    @Test
    void closeOfferRejectsAnOfferOfAnotherEstablishment() {
        when(offerRepository.findById(700L)).thenReturn(Optional.of(activeOffer(700L, 5)));

        assertThatThrownBy(() -> service.closeOffer(700L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- listagens ----------

    @Test
    void findAllForEstablishmentReturnsTheEstablishmentsOffers() {
        when(offerRepository.findByProductEstablishmentIdAndActiveTrueOrderByCreationDateDesc(1L))
                .thenReturn(List.of(activeOffer(701L, 5), activeOffer(700L, 3)));

        assertThat(service.findAllForEstablishment(1L))
                .extracting(OfferDTO::id).containsExactly(701L, 700L);
    }

    @Test
    void findOrderableAsksTheRepositoryForTheCurrentMoment() {
        when(offerRepository.findOrderable(NOW)).thenReturn(List.of(activeOffer(700L, 5)));

        assertThat(service.findOrderable()).extracting(OfferDTO::id).containsExactly(700L);
        verify(offerRepository).findOrderable(NOW);
    }

    // ---------- UC11 prioridade ----------

    @Test
    void markAsPriorityMarksAnActiveOfferAsPriority() {
        Offer offer = activeOffer(700L, 5);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        OfferDTO result = service.markAsPriority(700L, 1L);

        assertThat(result.priority()).isTrue();
        assertThat(offer.isPriority()).isTrue();
        verify(offerRepository).save(offer);
    }

    @Test
    void markAsPriorityRejectsInactiveOffer() {
        Offer offer = activeOffer(700L, 5);
        offer.setStatus(OfferStatus.CLOSED);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.markAsPriority(700L, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("current status is CLOSED");
    }

    @Test
    void markAsPriorityRejectsOfferFromAnotherEstablishment() {
        Offer offer = activeOffer(700L, 5);
        when(offerRepository.findById(700L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> service.markAsPriority(700L, 2L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void markOrPublishPriorityOfferPrioritizesExistingActiveOffer() {
        Offer existing = activeOffer(700L, 5);
        when(offerRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(10L, OfferStatus.ACTIVE))
                .thenReturn(Optional.of(existing));

        Optional<OfferDTO> result = service.markOrPublishPriorityOfferForProduct(10L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().priority()).isTrue();
        assertThat(existing.isPriority()).isTrue();
        verify(offerRepository).save(existing);
    }

    @Test
    void markOrPublishPriorityOfferPublishesNewPriorityOfferWhenNoneExists() {
        when(offerRepository.findFirstByProductIdAndStatusAndActiveTrueOrderByCreationDateDesc(10L, OfferStatus.ACTIVE))
                .thenReturn(Optional.empty());

        Optional<OfferDTO> result = service.markOrPublishPriorityOfferForProduct(10L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().priority()).isTrue();
    }

    @Test
    void markOrPublishPriorityOfferReturnsEmptyWhenStockIsZero() {
        product.setStockQuantity(0);

        Optional<OfferDTO> result = service.markOrPublishPriorityOfferForProduct(10L, 1L);

        assertThat(result).isEmpty();
        verify(offerRepository, never()).save(any(Offer.class));
    }
}