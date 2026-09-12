package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.validation.validators.ConsumerBusinessValidator;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.ConsumerDTO;
import br.com.seudominio.foodrescue.domain.dtos.LoginRequest;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.mappers.ConsumerMapper;
import br.com.seudominio.foodrescue.persistence.repositories.ConsumerRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

class ConsumerServiceTest {

    private ConsumerRepository consumerRepository;
    private ConsumerService consumerService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        consumerRepository = mock(ConsumerRepository.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        MessageUtils messageUtils = mock(MessageUtils.class);
        ConsumerBusinessValidator consumerValidator = new ConsumerBusinessValidator(validator, consumerRepository);
        passwordEncoder = new BCryptPasswordEncoder();
        consumerService = new ConsumerService(
                consumerRepository, new ConsumerMapper(), validator, messageUtils, consumerValidator, passwordEncoder);
    }

    @Test
    void registersConsumerWithHashedPassword() {
        when(consumerRepository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        when(consumerRepository.save(org.mockito.ArgumentMatchers.any(Consumer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ConsumerDTO result = consumerService.save(new ConsumerDTO(null, "Maria", "maria@example.com", "s3cret"));

        assertThat(result.name()).isEqualTo("Maria");
        assertThat(result.email()).isEqualTo("maria@example.com");
        assertThat(result.password()).isNull();
    }

    @Test
    void rejectsDuplicateEmailOnRegister() {
        Consumer existing = Consumer.builder().name("Maria").email("maria@example.com").passwordHash("hash").build();
        existing.setId(1L);
        when(consumerRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> consumerService.save(new ConsumerDTO(null, "Maria", "maria@example.com", "s3cret")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        Consumer consumer = Consumer.builder()
                .name("Maria")
                .email("maria@example.com")
                .passwordHash(passwordEncoder.encode("s3cret"))
                .build();
        when(consumerRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(consumer));

        ConsumerDTO result = consumerService.login(new LoginRequest("maria@example.com", "s3cret"));

        assertThat(result.email()).isEqualTo("maria@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        Consumer consumer = Consumer.builder()
                .name("Maria")
                .email("maria@example.com")
                .passwordHash(passwordEncoder.encode("s3cret"))
                .build();
        when(consumerRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(consumer));

        assertThatThrownBy(() -> consumerService.login(new LoginRequest("maria@example.com", "wrong")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        when(consumerRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consumerService.login(new LoginRequest("unknown@example.com", "whatever")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
