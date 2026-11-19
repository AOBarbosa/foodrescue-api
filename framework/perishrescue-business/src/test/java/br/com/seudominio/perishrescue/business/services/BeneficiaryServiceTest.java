package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.perishrescue.business.validation.validators.BeneficiaryBusinessValidator;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.dtos.BeneficiaryDTO;
import br.com.seudominio.perishrescue.domain.dtos.LoginRequest;
import br.com.seudominio.perishrescue.domain.entities.Beneficiary;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.mappers.BeneficiaryMapper;
import br.com.seudominio.perishrescue.persistence.repositories.BeneficiaryRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

class BeneficiaryServiceTest {

    private BeneficiaryRepository beneficiaryRepository;
    private BeneficiaryService beneficiaryService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        beneficiaryRepository = mock(BeneficiaryRepository.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        MessageUtils messageUtils = mock(MessageUtils.class);
        BeneficiaryBusinessValidator beneficiaryValidator = new BeneficiaryBusinessValidator(validator, beneficiaryRepository);
        passwordEncoder = new BCryptPasswordEncoder();
        beneficiaryService = new BeneficiaryService(
                beneficiaryRepository, new BeneficiaryMapper(), validator, messageUtils, beneficiaryValidator, passwordEncoder);
    }

    @Test
    void registersBeneficiaryWithHashedPassword() {
        when(beneficiaryRepository.findByEmail("maria@example.com")).thenReturn(Optional.empty());
        when(beneficiaryRepository.save(org.mockito.ArgumentMatchers.any(Beneficiary.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BeneficiaryDTO result = beneficiaryService.save(new BeneficiaryDTO(null, "Maria", "52998224725", "maria@example.com", "s3cret"));

        assertThat(result.name()).isEqualTo("Maria");
        assertThat(result.email()).isEqualTo("maria@example.com");
        assertThat(result.password()).isNull();
    }

    @Test
    void rejectsDuplicateEmailOnRegister() {
        Beneficiary existing = Beneficiary.builder().name("Maria").document("52998224725").email("maria@example.com").passwordHash("hash").build();
        existing.setId(1L);
        when(beneficiaryRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> beneficiaryService.save(new BeneficiaryDTO(null, "Maria", "52998224725", "maria@example.com", "s3cret")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        Beneficiary beneficiary = Beneficiary.builder()
                .name("Maria")
                .document("52998224725")
                .email("maria@example.com")
                .passwordHash(passwordEncoder.encode("s3cret"))
                .build();
        when(beneficiaryRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(beneficiary));

        BeneficiaryDTO result = beneficiaryService.login(new LoginRequest("maria@example.com", "s3cret"));

        assertThat(result.email()).isEqualTo("maria@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        Beneficiary beneficiary = Beneficiary.builder()
                .name("Maria")
                .document("52998224725")
                .email("maria@example.com")
                .passwordHash(passwordEncoder.encode("s3cret"))
                .build();
        when(beneficiaryRepository.findByEmail("maria@example.com")).thenReturn(Optional.of(beneficiary));

        assertThatThrownBy(() -> beneficiaryService.login(new LoginRequest("maria@example.com", "wrong")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        when(beneficiaryRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> beneficiaryService.login(new LoginRequest("unknown@example.com", "whatever")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
