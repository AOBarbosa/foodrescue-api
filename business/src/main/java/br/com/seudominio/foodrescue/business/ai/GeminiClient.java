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

    /**
     * Constructor.
     *
     * @param apiKey         the Gemini API key; blank disables the client
     * @param model          the model id, e.g. {@code gemini-3.8-flash}
     * @param baseUrl        the Gemini API base URL
     * @param timeoutSeconds the maximum time to wait for an answer
     */
    @Autowired
    public GeminiClient(
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            @Value("${app.ai.gemini.model:gemini-3.8-flash}") String model,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${app.ai.gemini.timeout-seconds:15}") long timeoutSeconds) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(),
                apiKey, model, baseUrl, Duration.ofSeconds(timeoutSeconds));
    }

    GeminiClient(HttpClient httpClient, String apiKey, String model, String baseUrl, Duration timeout) {
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
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

        HttpResponse<String> response = send(body);
        if (response.statusCode() != 200) {
            throw new GeminiException("Gemini returned HTTP " + response.statusCode() + ": " + response.body());
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
