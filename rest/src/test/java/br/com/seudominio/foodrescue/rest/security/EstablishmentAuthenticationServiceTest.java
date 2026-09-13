package br.com.seudominio.foodrescue.rest.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.services.EstablishmentService;
import br.com.seudominio.foodrescue.domain.dtos.EstablishmentDTO;
import br.com.seudominio.foodrescue.domain.dtos.LoginRequest;
import br.com.seudominio.foodrescue.domain.enums.EstablishmentCategory;
import br.com.seudominio.foodrescue.rest.dtos.EstablishmentAuthResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EstablishmentAuthenticationServiceTest {

    private EstablishmentService establishmentService;
    private JwtTokenProvider jwtTokenProvider;
    private EstablishmentAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        establishmentService = mock(EstablishmentService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authenticationService = new EstablishmentAuthenticationService(establishmentService, jwtTokenProvider);
    }

    private EstablishmentDTO establishmentDto() {
        return new EstablishmentDTO(
                1L, "Padaria da Esquina", "11222333000181", "Rua das Flores, 123",
                EstablishmentCategory.BAKERY, "padaria@example.com", null);
    }

    @Test
    void registerSavesAndIssuesTokenForEstablishmentRole() {
        EstablishmentDTO dto = establishmentDto();
        when(establishmentService.save(dto)).thenReturn(dto);
        when(jwtTokenProvider.generateToken(1L, UserRole.ESTABLISHMENT)).thenReturn("token-123");

        EstablishmentAuthResponse response = authenticationService.register(dto);

        assertThat(response.establishment()).isEqualTo(dto);
        assertThat(response.token()).isEqualTo("token-123");
    }

    @Test
    void loginAuthenticatesAndIssuesTokenForEstablishmentRole() {
        LoginRequest request = new LoginRequest("padaria@example.com", "s3cret");
        EstablishmentDTO dto = establishmentDto();
        when(establishmentService.login(request)).thenReturn(dto);
        when(jwtTokenProvider.generateToken(1L, UserRole.ESTABLISHMENT)).thenReturn("token-456");

        EstablishmentAuthResponse response = authenticationService.login(request);

        assertThat(response.establishment()).isEqualTo(dto);
        assertThat(response.token()).isEqualTo("token-456");
    }
}
