package br.com.seudominio.perishrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalTime;

import br.com.seudominio.perishrescue.business.validation.validators.OffererBusinessValidator;
import br.com.seudominio.perishrescue.core.utils.MessageUtils;
import br.com.seudominio.perishrescue.core.validation.exception.ValidationException;
import br.com.seudominio.perishrescue.domain.dtos.OffererDTO;
import br.com.seudominio.perishrescue.domain.dtos.OffererUpdateDTO;
import br.com.seudominio.perishrescue.domain.dtos.LoginRequest;
import br.com.seudominio.perishrescue.domain.entities.Offerer;
import br.com.seudominio.perishrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.perishrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.perishrescue.domain.mappers.OffererMapper;
import br.com.seudominio.perishrescue.persistence.repositories.OffererRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

class OffererServiceTest {

    private static final String VALID_CNPJ = "11222333000181";

    private OffererRepository offererRepository;
    private OffererService offererService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        offererRepository = mock(OffererRepository.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        MessageUtils messageUtils = mock(MessageUtils.class);
        OffererBusinessValidator offererValidator =
                new OffererBusinessValidator(validator, offererRepository);
        passwordEncoder = new BCryptPasswordEncoder();
        offererService = new OffererService(
                offererRepository,
                new OffererMapper(),
                validator,
                messageUtils,
                offererValidator,
                passwordEncoder);
    }

    private OffererDTO validDto() {
        return new OffererDTO(
                null,
                "Padaria da Esquina",
                VALID_CNPJ,
                "Rua das Flores, 123",
                "BAKERY", LocalTime.of(7, 0), LocalTime.of(22, 0),
                "padaria@example.com",
                "s3cret");
    }

    @Test
    void registersOffererWithHashedPassword() {
        when(offererRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(offererRepository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        when(offererRepository.save(org.mockito.ArgumentMatchers.any(Offerer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffererDTO result = offererService.save(validDto());

        assertThat(result.name()).isEqualTo("Padaria da Esquina");
        assertThat(result.cnpj()).isEqualTo(VALID_CNPJ);
        assertThat(result.email()).isEqualTo("padaria@example.com");
        assertThat(result.password()).isNull();
    }

    @Test
    void rejectsDuplicateCnpjOnRegister() {
        Offerer existing = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("outro@example.com")
                .passwordHash("hash")
                .build();
        existing.setId(1L);
        when(offererRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(offererRepository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offererService.save(validDto()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsDuplicateEmailOnRegister() {
        Offerer existing = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj("98765432000198")
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hash")
                .build();
        existing.setId(1L);
        when(offererRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(offererRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> offererService.save(validDto()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        Offerer offerer = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash(passwordEncoder.encode("s3cret"))
                .build();
        when(offererRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(offerer));

        OffererDTO result = offererService.login(new LoginRequest("padaria@example.com", "s3cret"));

        assertThat(result.email()).isEqualTo("padaria@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        Offerer offerer = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash(passwordEncoder.encode("s3cret"))
                .build();
        when(offererRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(offerer));

        assertThatThrownBy(() -> offererService.login(new LoginRequest("padaria@example.com", "wrong")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        when(offererRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offererService.login(new LoginRequest("unknown@example.com", "whatever")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void getByIdReturnsOffererWithoutPassword() {
        Offerer offerer = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hash")
                .build();
        offerer.setId(1L);
        when(offererRepository.findById(1L)).thenReturn(Optional.of(offerer));

        OffererDTO result = offererService.getById(1L);

        assertThat(result.name()).isEqualTo("Padaria da Esquina");
        assertThat(result.password()).isNull();
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(offererRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offererService.getById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateProfileUpdatesFieldsAndKeepsPasswordWhenBlank() {
        Offerer existing = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("original-hash")
                .build();
        existing.setId(1L);
        when(offererRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offererRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(offererRepository.findByEmail("nova@example.com")).thenReturn(Optional.empty());
        when(offererRepository.save(org.mockito.ArgumentMatchers.any(Offerer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffererUpdateDTO update = new OffererUpdateDTO(
                "Novo Nome", VALID_CNPJ, "Rua Nova, 456", "MARKET", LocalTime.of(7, 0), LocalTime.of(22, 0), "nova@example.com", null);

        OffererDTO result = offererService.updateProfile(1L, update);

        assertThat(result.name()).isEqualTo("Novo Nome");
        assertThat(result.address()).isEqualTo("Rua Nova, 456");
        assertThat(result.category()).isEqualTo("MARKET");
        assertThat(result.email()).isEqualTo("nova@example.com");
        assertThat(existing.getPasswordHash()).isEqualTo("original-hash");
    }

    @Test
    void updateProfileHashesNewPasswordWhenProvided() {
        Offerer existing = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("original-hash")
                .build();
        existing.setId(1L);
        when(offererRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offererRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(offererRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));
        when(offererRepository.save(org.mockito.ArgumentMatchers.any(Offerer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffererUpdateDTO update = new OffererUpdateDTO(
                "Padaria da Esquina", VALID_CNPJ, "Rua das Flores, 123", "BAKERY", LocalTime.of(7, 0), LocalTime.of(22, 0),
                "padaria@example.com", "n3wpass");

        offererService.updateProfile(1L, update);

        assertThat(existing.getPasswordHash()).isNotEqualTo("original-hash");
        assertThat(passwordEncoder.matches("n3wpass", existing.getPasswordHash())).isTrue();
    }

    @Test
    void updateProfileRejectsCnpjAlreadyUsedByAnotherOfferer() {
        Offerer existing = Offerer.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category("BAKERY")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("padaria@example.com")
                .passwordHash("hash")
                .build();
        existing.setId(1L);
        Offerer other = Offerer.builder()
                .name("Outro")
                .cnpj("98765432000198")
                .address("Rua B, 2")
                .category("MARKET")
                .businessHours(LocalTime.of(7, 0), LocalTime.of(22, 0))
                .email("outro@example.com")
                .passwordHash("hash2")
                .build();
        other.setId(2L);
        when(offererRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(offererRepository.findByCnpj("98765432000198")).thenReturn(Optional.of(other));
        when(offererRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));

        OffererUpdateDTO update = new OffererUpdateDTO(
                "Padaria da Esquina", "98765432000198", "Rua das Flores, 123", "BAKERY", LocalTime.of(7, 0), LocalTime.of(22, 0),
                "padaria@example.com", null);

        assertThatThrownBy(() -> offererService.updateProfile(1L, update))
                .isInstanceOf(ValidationException.class);
    }
}
