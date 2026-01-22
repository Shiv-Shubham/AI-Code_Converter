package com.example.code_converter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;
import java.util.function.Consumer;
@Service
public class AiConversionService {

    @Value("${gemini.api-key:}")
    private String geminiKey;

    @Value("${groq.api-key:}")
    private String groqKey;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaUrl;

    private final WebClient webClient = WebClient.builder().build();

    public void convertStream(String to, String code, Consumer<String> onChunk) {

        //  Try local Ollama first
        try {
            System.out.println(">>> Using OLLAMA");
            streamFromOllama(to, code, onChunk);
            return;
        } catch (Exception e) {
            System.out.println("Ollama failed, trying Gemini...");
        }
        //  Try Grok
        if (groqKey != null && !groqKey.isBlank()) {
            try {
                System.out.println(">>> Using GROQ");
                streamFromGrok(to, code, onChunk);
                return;
            } catch (Exception e) {
                System.out.println("Grok failed.");
            }
        }

        // Try Gemini
        if (geminiKey != null && !geminiKey.isBlank()) {
            try {
                System.out.println(">>> Using GEMINI");
                streamFromGemini(to, code, onChunk);
                return;
            } catch (Exception e) {
                System.out.println("Gemini failed, trying Groq...");
            }
        }


        //  Final fallback
        onChunk.accept("\n\n All free limits reached or services unavailable. Please try again later.");
    }

    private String buildPrompt(String to, String code) {
        return """
Convert the following code to %s.

Rules:
- Output ONLY the converted code
- No explanation
- No markdown
- Proper formatting

Code:
%s
""".formatted(to, code);
    }

    // ---------------- OLLAMA (LOCAL) ----------------
    private void streamFromOllama(String to, String code, Consumer<String> onChunk) {

        Map<String, Object> body = Map.of(
                "model", "qwen2.5:3b",
                "prompt", buildPrompt(to, code),
                "stream", true
        );

        webClient.post()
                .uri(ollamaUrl + "/api/generate")
                .header("Accept", "application/x-ndjson")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(chunk -> reactor.core.publisher.Flux.fromArray(chunk.split("\n")))
                .doOnNext(line -> {
                    if (line == null || line.isBlank()) return;

                    int idx = line.indexOf("\"response\":");
                    if (idx != -1) {
                        int start = line.indexOf('"', idx + 11);
                        int end = line.indexOf('"', start + 1);
                        if (start != -1 && end != -1) {
                            String token = line.substring(start + 1, end);
                            onChunk.accept(token);
                        }
                    }
                })
                .blockLast();
    }

    // ---------------- GEMINI ----------------
    private void streamFromGemini(String to, String code, Consumer<String> onChunk) {

        Map<String, Object> body = Map.of(
                "contents", new Object[]{
                        Map.of("parts", new Object[]{
                                Map.of("text", buildPrompt(to, code))
                        })
                }
        );

        webClient.post()
                .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnNext(resp -> {
                    // Very simple extraction (non-stream for now)
                    var candidates = (java.util.List<Map<String, Object>>) resp.get("candidates");
                    var content = (Map<String, Object>) candidates.get(0).get("content");
                    var parts = (java.util.List<Map<String, Object>>) content.get("parts");
                    String text = (String) parts.get(0).get("text");
                    onChunk.accept(text);
                })
                .block();
    }

    // ---------------- GROK ----------------
    private void streamFromGrok(String to, String code, Consumer<String> onChunk) {

        Map<String, Object> body = Map.of(
                "model", "llama-3.3-70b-versatile",
                "messages", new Object[]{
                        Map.of("role", "user", "content", buildPrompt(to, code))
                },
                "temperature", 0.1,
                "max_tokens", 2048
        );

        webClient.post()
                .uri("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer " + groqKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.value() == 400, resp ->
                        resp.bodyToMono(String.class).map(err -> new RuntimeException("Groq 400: " + err))
                )
                .bodyToMono(Map.class)
                .doOnNext(resp -> {
                    var choices = (java.util.List<Map<String, Object>>) resp.get("choices");
                    var msg = (Map<String, Object>) choices.get(0).get("message");
                    String text = (String) msg.get("content");
                    onChunk.accept(text);
                })
                .block();
    }
}


