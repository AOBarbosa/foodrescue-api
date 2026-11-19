package br.com.seudominio.perishrescue.rest.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalTime;

import br.com.seudominio.perishrescue.business.services.OffererService;
import br.com.seudominio.perishrescue.domain.dtos.OffererDTO;
import br.com.seudominio.perishrescue.domain.dtos.LoginRequest;
import br.com.seudominio.perishrescue.rest.dtos.OffererAuthResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OffererAuthenticationServiceTest {

    private OffererService offererService;
    private JwtTokenProvider jwtTokenProvider;
    private OffererAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        offererService = mock(OffererService.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);
        authenticationService = new OffererAuthenticationService(offererService, jwtTokenProvider);
    }

    private OffererDTO offererDto() {
        return new OffererDTO(
                1L, "Padaria da Esquina", "11222333000181", "Rua das Flores, 123",
                "BAKERY", LocalTime.of(7, 0), LocalTime.of(22, 0), "padaria@example.com", null);
    }

    @Test
    void registerSavesAndIssuesTokenForOffererRole() {
        OffererDTO dto = offererDto();
        when(offererService.save(dto)).thenReturn(dto);
        when(jwtTokenProvider.generateToken(1L, UserRole.OFFERER)).thenReturn("token-123");

        OffererAuthResponse response = authenticationService.register(dto);

        assertThat(response.offerer()).isEqualTo(dto);
        assertThat(response.token()).isEqualTo("token-123");
    }

    @Test
    void loginAuthenticatesAndIssuesTokenForOffererRole() {
        LoginRequest request = new LoginRequest("padaria@example.com", "s3cret");
        OffererDTO dto = offererDto();
        when(offererService.login(request)).thenReturn(dto);
        when(jwtTokenProvider.generateToken(1L, UserRole.OFFERER)).thenReturn("token-456");

        OffererAuthResponse response = authenticationService.login(request);

        assertThat(response.offerer()).isEqualTo(dto);
        assertThat(response.token()).isEqualTo("token-456");
    }
}
