package br.com.seudominio.foodrescue.business.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

class GeminiClientTest {

    private static final Map<String, Object> SCHEMA = Map.of("type", "OBJECT");

    private HttpServer server;
    private final AtomicReference<String> receivedKey = new AtomicReference<>();
    private final AtomicReference<String> receivedPath = new AtomicReference<>();
    private final AtomicReference<String> receivedBody = new AtomicReference<>();
    private final AtomicInteger requestCount = new AtomicInteger();
    private final Deque<Integer> statuses = new ArrayDeque<>();
    private int status;
    private String responseBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            receivedKey.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            receivedPath.set(exchange.getRequestURI().getPath());
            receivedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestCount.incrementAndGet();
            int currentStatus = statuses.isEmpty() ? status : statuses.poll();
            byte[] bytes = (currentStatus == 200 ? responseBody : "{\"error\":{\"message\":\"high demand\"}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(currentStatus, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private GeminiClient client(String apiKey) {
        return new GeminiClient(HttpClient.newHttpClient(), apiKey, "gemini-test",
                "http://localhost:" + server.getAddress().getPort(), Duration.ofSeconds(5), 2, Duration.ZERO);
    }

    @Test
    void returnsParsedJsonAnswer() {
        status = 200;
        responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"predictedQuantity\\\": 7}\"}]}}]}";

        JsonNode answer = client("secret").generateJson("prompt", SCHEMA);

        assertThat(answer.path("predictedQuantity").asInt()).isEqualTo(7);
        assertThat(receivedKey.get()).isEqualTo("secret");
        assertThat(receivedPath.get()).isEqualTo("/models/gemini-test:generateContent");
        assertThat(receivedBody.get()).contains("\"responseMimeType\":\"application/json\"").contains("prompt");
    }

    @Test
    void retriesServerFailuresAndSucceeds() {
        statuses.add(503);
        statuses.add(500);
        status = 200;
        responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"predictedQuantity\\\": 3}\"}]}}]}";

        JsonNode answer = client("secret").generateJson("prompt", SCHEMA);

        assertThat(answer.path("predictedQuantity").asInt()).isEqualTo(3);
        assertThat(requestCount.get()).isEqualTo(3);
    }

    @Test
    void failsWithApiErrorMessageAfterExhaustingRetries() {
        status = 503;

        assertThatThrownBy(() -> client("secret").generateJson("prompt", SCHEMA))
                .isInstanceOf(GeminiException.class)
                .hasMessage("Gemini returned HTTP 503: high demand");
        assertThat(requestCount.get()).isEqualTo(3);
    }

    @Test
    void doesNotRetryClientErrors() {
        status = 400;

        assertThatThrownBy(() -> client("secret").generateJson("prompt", SCHEMA))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("400");
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void doesNotRetryQuotaExceeded() {
        status = 429;

        assertThatThrownBy(() -> client("secret").generateJson("prompt", SCHEMA))
                .isInstanceOf(GeminiException.class)
                .hasMessageContaining("429");
        assertThat(requestCount.get()).isEqualTo(1);
    }

    @Test
    void failsOnNonJsonAnswer() {
        status = 200;
        responseBody = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"not json\"}]}}]}";

        assertThatThrownBy(() -> client("secret").generateJson("prompt", SCHEMA))
                .isInstanceOf(GeminiException.class);
    }

    @Test
    void isNotConfiguredWithoutApiKey() {
        GeminiClient client = client("");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.generateJson("prompt", SCHEMA)).isInstanceOf(GeminiException.class);
    }
}
