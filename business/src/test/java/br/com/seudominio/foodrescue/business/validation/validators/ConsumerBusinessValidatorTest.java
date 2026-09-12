package br.com.seudominio.foodrescue.business.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.core.validation.BusinessOperation;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.persistence.repositories.ConsumerRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

class ConsumerBusinessValidatorTest {

    private final Validator beanValidator = Validation.buildDefaultValidatorFactory().getValidator();

    private Consumer validConsumer() {
        Consumer consumer = Consumer.builder()
                .name("Maria")
                .email("maria@example.com")
                .passwordHash("hashed-password")
                .build();
        consumer.setCreationDate(LocalDateTime.now());
        return consumer;
    }

    @Test
    void passesForNewConsumerWithUniqueEmail() {
        ConsumerRepository repository = mock(ConsumerRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        ConsumerBusinessValidator validator = new ConsumerBusinessValidator(beanValidator, repository);

        validator.validateOperation(validConsumer(), BusinessOperation.CREATE);
    }

    @Test
    void rejectsEmailAlreadyUsedByAnotherConsumer() {
        Consumer existing = validConsumer();
        existing.setId(1L);
        ConsumerRepository repository = mock(ConsumerRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.of(existing));
        ConsumerBusinessValidator validator = new ConsumerBusinessValidator(beanValidator, repository);

        Consumer newConsumer = validConsumer();

        assertThatThrownBy(() -> validator.validateOperation(newConsumer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class)
                .satisfies(ex -> assertThat(((ValidationException) ex).getErrors())
                        .anySatisfy(error -> assertThat(error.code()).isEqualTo("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    void allowsEmailToMatchTheEntityItself() {
        Consumer existing = validConsumer();
        existing.setId(1L);
        ConsumerRepository repository = mock(ConsumerRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.of(existing));
        ConsumerBusinessValidator validator = new ConsumerBusinessValidator(beanValidator, repository);

        validator.validateOperation(existing, BusinessOperation.UPDATE);
    }

    @Test
    void rejectsBlankName() {
        ConsumerRepository repository = mock(ConsumerRepository.class);
        when(repository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        ConsumerBusinessValidator validator = new ConsumerBusinessValidator(beanValidator, repository);

        Consumer consumer = validConsumer();
        consumer.setName(" ");

        assertThatThrownBy(() -> validator.validateOperation(consumer, BusinessOperation.CREATE))
                .isInstanceOf(ValidationException.class);
    }
}
