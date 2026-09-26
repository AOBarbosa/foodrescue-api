package br.com.seudominio.foodrescue.business.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin client for the Gemini {@code generateContent} endpoint, returning the
 * model's answer as a JSON document that follows a given response schema.
 * Reusable by every AI-backed use case (UC05, UC07, UC10).
 *
 * <p>Transient server failures (HTTP 5xx, e.g. 503 "high demand") are retried
 * with exponential backoff before giving up. HTTP 429 (quota exceeded) is not
 * retried: another attempt within the same minute would only consume more
 * quota.</p>
 *
 * <p>Configured through {@code app.ai.gemini.*}; the API key comes from the
 * {@code GEMINI_API_KEY} environment variable and is never committed.</p>
 *
 * @author Andre Barbosa
 * @since 1.0.0
 */
@Component
public class GeminiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final Duration timeout;
    private final int maxRetries;
    private final Duration initialBackoff;

    /**
     * Constructor.
     *
     * @param apiKey         the Gemini API key; blank disables the client
     * @param model          the model id, e.g. {@code gemini-3.8-flash}
     * @param baseUrl        the Gemini API base URL
     * @param timeoutSeconds the maximum time to wait for each answer
     * @param maxRetries     how many times a transient server failure (5xx) is retried
     * @param backoffMillis  wait before the first retry; doubles on each new retry
     */
    @Autowired
    public GeminiClient(
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-3.8-flash}") String model,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${app.ai.gemini.timeout-seconds:15}") long timeoutSeconds,
            @Value("${app.ai.gemini.max-retries:2}") int maxRetries,
            @Value("${app.ai.gemini.backoff-millis:1000}") long backoffMillis) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                apiKey, model, baseUrl, Duration.ofSeconds(timeoutSeconds),
                maxRetries, Duration.ofMillis(backoffMillis));
    }

    GeminiClient(HttpClient httpClient, String apiKey, String model, String baseUrl, Duration timeout,
                 int maxRetries, Duration initialBackoff) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
    }

    /**
     * Tells whether an API key was configured.
     *
     * @return {@code true} if the client can call the API
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Returns the configured model id.
     *
     * @return the model id
     */
    public String getModel() {
        return model;
    }

    /**
     * Sends a prompt and returns the model's JSON answer.
     *
     * @param prompt         the prompt text
     * @param responseSchema the OpenAPI-style schema the answer must follow
     * @return the parsed JSON answer
     * @throws GeminiException if the client is not configured, the call fails or the answer is not valid JSON
     */
    public JsonNode generateJson(String prompt, Map<String, Object> responseSchema) {
        if (!isConfigured()) {
            throw new GeminiException("Gemini API key is not configured");
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "temperature", 0.2,
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema));

        HttpResponse<String> response = sendWithRetry(body);
        if (response.statusCode() != 200) {
            throw new GeminiException("Gemini returned HTTP " + response.statusCode() + ": "
                    + errorMessage(response.body()));
        }

        try {
            JsonNode text = objectMapper.readTree(response.body())
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!text.isTextual()) {
                throw new GeminiException("Gemini answer has no text content");
            }
            return objectMapper.readTree(text.asText());
        } catch (JsonProcessingException e) {
            throw new GeminiException("Gemini answer is not valid JSON", e);
        }
    }

    private HttpResponse<String> sendWithRetry(Map<String, Object> body) {
        HttpResponse<String> response = send(body);
        Duration backoff = initialBackoff;
        for (int retry = 0; retry < maxRetries && isTransient(response.statusCode()); retry++) {
            sleep(backoff);
            backoff = backoff.multipliedBy(2);
            response = send(body);
        }
        return response;
    }

    private boolean isTransient(int statusCode) {
        return statusCode >= 500;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiException("Gemini call was interrupted", e);
        }
    }

    private String errorMessage(String responseBody) {
        try {
            JsonNode message = objectMapper.readTree(responseBody).path("error").path("message");
            if (message.isTextual()) {
                return message.asText();
            }
        } catch (JsonProcessingException e) {
            // not a JSON error payload; fall through to the raw body
        }
        return responseBody;
    }

    private HttpResponse<String> send(Map<String, Object> body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/models/" + model + ":generateContent"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new GeminiException("Gemini call failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiException("Gemini call was interrupted", e);
        }
    }
}
