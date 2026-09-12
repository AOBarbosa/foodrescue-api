package br.com.seudominio.foodrescue.rest.security;

import br.com.seudominio.foodrescue.business.helpers.MessageCode;
import br.com.seudominio.foodrescue.rest.dtos.ApiError;
import br.com.seudominio.foodrescue.rest.dtos.ApiResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Writes a JSON 403 response (same {@link ApiResponse} shape as every other
 * endpoint) when an authenticated request is rejected for having the wrong
 * role. Runs at the security-filter level, so it can't rely on
 * {@code @ExceptionHandler}.
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Not wired from the Spring context: this filter-level handler runs before
     * Spring MVC's message converters are involved, so it needs its own mapper.
     */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public void handle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        ApiError apiError = new ApiError(HttpStatus.FORBIDDEN, "Access denied", MessageCode.ACCESS_DENIED);
        response.setStatus(apiError.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ApiResponse<>(apiError, apiError.getMessage(), false, apiError.getMessageCode()));
    }
}
