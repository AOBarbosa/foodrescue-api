package br.com.seudominio.foodrescue.business.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.validation.validators.EstablishmentBusinessValidator;
import br.com.seudominio.foodrescue.core.utils.MessageUtils;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentDTO;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentUpdateDTO;
import br.com.seudominio.foodrescue.domain.dtos.LoginRequest;
import br.com.seudominio.foodrescue.domain.entities.Establishment;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.domain.mappers.EstablishmentMapper;
import br.com.seudominio.foodrescue.persistence.repositories.EstablishmentRepository;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

class EstablishmentServiceTest {

    private static final String VALID_CNPJ = "11222333000181";

    private EstablishmentRepository establishmentRepository;
    private EstablishmentService establishmentService;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        establishmentRepository = mock(EstablishmentRepository.class);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        MessageUtils messageUtils = mock(MessageUtils.class);
        EstablishmentBusinessValidator establishmentValidator =
                new EstablishmentBusinessValidator(validator, establishmentRepository);
        passwordEncoder = new BCryptPasswordEncoder();
        establishmentService = new EstablishmentService(
                establishmentRepository,
                new EstablishmentMapper(),
                validator,
                messageUtils,
                establishmentValidator,
                passwordEncoder);
    }

    private EstablishmentDTO validDto() {
        return new EstablishmentDTO(
                null,
                "Padaria da Esquina",
                VALID_CNPJ,
                "Rua das Flores, 123",
                EstablishmentCategory.BAKERY,
                "padaria@example.com",
                "s3cret");
    }

    @Test
    void registersEstablishmentWithHashedPassword() {
        when(establishmentRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(establishmentRepository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());
        when(establishmentRepository.save(org.mockito.ArgumentMatchers.any(Establishment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EstablishmentDTO result = establishmentService.save(validDto());

        assertThat(result.name()).isEqualTo("Padaria da Esquina");
        assertThat(result.cnpj()).isEqualTo(VALID_CNPJ);
        assertThat(result.email()).isEqualTo("padaria@example.com");
        assertThat(result.password()).isNull();
    }

    @Test
    void rejectsDuplicateCnpjOnRegister() {
        Establishment existing = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("outro@example.com")
                .passwordHash("hash")
                .build();
        existing.setId(1L);
        when(establishmentRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(establishmentRepository.findByEmail("padaria@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> establishmentService.save(validDto()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsDuplicateEmailOnRegister() {
        Establishment existing = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj("98765432000198")
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hash")
                .build();
        existing.setId(1L);
        when(establishmentRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.empty());
        when(establishmentRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> establishmentService.save(validDto()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void loginSucceedsWithCorrectCredentials() {
        Establishment establishment = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash(passwordEncoder.encode("s3cret"))
                .build();
        when(establishmentRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(establishment));

        EstablishmentDTO result = establishmentService.login(new LoginRequest("padaria@example.com", "s3cret"));

        assertThat(result.email()).isEqualTo("padaria@example.com");
    }

    @Test
    void loginFailsWithWrongPassword() {
        Establishment establishment = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash(passwordEncoder.encode("s3cret"))
                .build();
        when(establishmentRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(establishment));

        assertThatThrownBy(() -> establishmentService.login(new LoginRequest("padaria@example.com", "wrong")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void loginFailsWithUnknownEmail() {
        when(establishmentRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> establishmentService.login(new LoginRequest("unknown@example.com", "whatever")))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void getByIdReturnsEstablishmentWithoutPassword() {
        Establishment establishment = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hash")
                .build();
        establishment.setId(1L);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(establishment));

        EstablishmentDTO result = establishmentService.getById(1L);

        assertThat(result.name()).isEqualTo("Padaria da Esquina");
        assertThat(result.password()).isNull();
    }

    @Test
    void getByIdThrowsWhenNotFound() {
        when(establishmentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> establishmentService.getById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void updateProfileUpdatesFieldsAndKeepsPasswordWhenBlank() {
        Establishment existing = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("original-hash")
                .build();
        existing.setId(1L);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(establishmentRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(establishmentRepository.findByEmail("nova@example.com")).thenReturn(Optional.empty());
        when(establishmentRepository.save(org.mockito.ArgumentMatchers.any(Establishment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EstablishmentUpdateDTO update = new EstablishmentUpdateDTO(
                "Novo Nome", VALID_CNPJ, "Rua Nova, 456", EstablishmentCategory.MARKET, "nova@example.com", null);

        EstablishmentDTO result = establishmentService.updateProfile(1L, update);

        assertThat(result.name()).isEqualTo("Novo Nome");
        assertThat(result.address()).isEqualTo("Rua Nova, 456");
        assertThat(result.category()).isEqualTo(EstablishmentCategory.MARKET);
        assertThat(result.email()).isEqualTo("nova@example.com");
        assertThat(existing.getPasswordHash()).isEqualTo("original-hash");
    }

    @Test
    void updateProfileHashesNewPasswordWhenProvided() {
        Establishment existing = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("original-hash")
                .build();
        existing.setId(1L);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(establishmentRepository.findByCnpj(VALID_CNPJ)).thenReturn(Optional.of(existing));
        when(establishmentRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));
        when(establishmentRepository.save(org.mockito.ArgumentMatchers.any(Establishment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EstablishmentUpdateDTO update = new EstablishmentUpdateDTO(
                "Padaria da Esquina", VALID_CNPJ, "Rua das Flores, 123", EstablishmentCategory.BAKERY,
                "padaria@example.com", "n3wpass");

        establishmentService.updateProfile(1L, update);

        assertThat(existing.getPasswordHash()).isNotEqualTo("original-hash");
        assertThat(passwordEncoder.matches("n3wpass", existing.getPasswordHash())).isTrue();
    }

    @Test
    void updateProfileRejectsCnpjAlreadyUsedByAnotherEstablishment() {
        Establishment existing = Establishment.builder()
                .name("Padaria da Esquina")
                .cnpj(VALID_CNPJ)
                .address("Rua das Flores, 123")
                .category(EstablishmentCategory.BAKERY)
                .email("padaria@example.com")
                .passwordHash("hash")
                .build();
        existing.setId(1L);
        Establishment other = Establishment.builder()
                .name("Outro")
                .cnpj("98765432000198")
                .address("Rua B, 2")
                .category(EstablishmentCategory.MARKET)
                .email("outro@example.com")
                .passwordHash("hash2")
                .build();
        other.setId(2L);
        when(establishmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(establishmentRepository.findByCnpj("98765432000198")).thenReturn(Optional.of(other));
        when(establishmentRepository.findByEmail("padaria@example.com")).thenReturn(Optional.of(existing));

        EstablishmentUpdateDTO update = new EstablishmentUpdateDTO(
                "Padaria da Esquina", "98765432000198", "Rua das Flores, 123", EstablishmentCategory.BAKERY,
                "padaria@example.com", null);

        assertThatThrownBy(() -> establishmentService.updateProfile(1L, update))
                .isInstanceOf(ValidationException.class);
    }
}
