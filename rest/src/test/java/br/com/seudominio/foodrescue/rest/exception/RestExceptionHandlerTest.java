package br.com.seudominio.foodrescue.rest.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.seudominio.foodrescue.business.helpers.MessageCode;
import br.com.seudominio.foodrescue.core.validation.ValidationError;
import br.com.seudominio.foodrescue.core.validation.exception.ValidationException;
import br.com.seudominio.foodrescue.domain.entities.Consumer;
import br.com.seudominio.foodrescue.domain.exception.BusinessRuleViolationException;
import br.com.seudominio.foodrescue.domain.exception.DuplicateEntityException;
import br.com.seudominio.foodrescue.domain.exception.EntityNotFoundException;
import br.com.seudominio.foodrescue.rest.dtos.ApiError;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    private HttpServletRequest anyRequest() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/consumers");
        return request;
    }

    @Test
    void mapsEntityNotFoundTo404() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleEntityNotFound(new EntityNotFoundException(Consumer.class, 1L));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo(MessageCode.ENTITY_NOT_FOUND);
    }

    @Test
    void mapsDuplicateEntityTo409() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleDuplicateEntity(new DuplicateEntityException("email already registered"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo(MessageCode.DUPLICATE_ENTITY);
    }

    @Test
    void mapsBusinessRuleViolationTo422() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleBusinessRuleViolation(new BusinessRuleViolationException("invalid email or password"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody().code()).isEqualTo(MessageCode.BUSINESS_RULE_VIOLATION);
    }

    @Test
    void mapsValidationExceptionTo422WithSubErrors() {
        ValidationException ex = new ValidationException(
                "Validation failed",
                List.of(new ValidationError("email already registered", "email", "a@b.com", "EMAIL_ALREADY_EXISTS")));

        ResponseEntity<ApiResponse<Object>> response = handler.handleValidationException(ex, anyRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        ApiError apiError = (ApiError) response.getBody().data();
        assertThat(apiError.getSubErrors()).hasSize(1);
        assertThat(apiError.getSubErrors().get(0).getCode()).isEqualTo("EMAIL_ALREADY_EXISTS");
    }

    @Test
    void mapsIllegalArgumentTo400() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleIllegalArgument(new IllegalArgumentException("bad input"), anyRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsGenericExceptionTo500WithoutLeakingMessage() {
        ResponseEntity<ApiResponse<Object>> response =
                handler.handleGenericException(new RuntimeException("some internal detail"), anyRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiError apiError = (ApiError) response.getBody().data();
        assertThat(apiError.getMessage()).doesNotContain("some internal detail");
    }
}
