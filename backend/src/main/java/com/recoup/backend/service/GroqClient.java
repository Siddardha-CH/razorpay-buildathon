package com.recoup.backend.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Thin, optional LLM boundary over Groq's OpenAI-compatible chat completions
 *  endpoint. Every call is advisory text generation only (diagnosis narration,
 *  message copy) -- never a money-moving decision; see PolicyEngine for that.
 *  If no API key is configured, callers fall back to deterministic templates
 *  so the whole pipeline runs offline -- which is also what the test suite
 *  exercises. */
@Service
public class GroqClient {

    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${groq.api-key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    public Optional<String> complete(String prompt, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        try {
            ObjectNode body = mapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", maxTokens);
            body.put("temperature", 0.3);
            var messages = mapper.createArrayNode();
            ObjectNode message = mapper.createObjectNode();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            body.set("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }
            JsonNode root = mapper.readTree(response.body());
            String text = root.path("choices").path(0).path("message").path("content").asText(null);
            return Optional.ofNullable(text).map(String::trim);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
