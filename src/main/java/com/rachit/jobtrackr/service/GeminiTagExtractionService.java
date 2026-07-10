package com.rachit.jobtrackr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class GeminiTagExtractionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiTagExtractionService.class);

    private static final String EXTRACTION_PROMPT = """
            You are a technical recruiter assistant. Extract all required technical skills,
            programming languages, frameworks, tools, and technologies from the job description below.
            
            Rules:
            - Return ONLY a JSON array of strings, no explanation, no markdown, no code block.
            - Each element should be a concise skill name (e.g. "Java", "Spring Boot", "PostgreSQL").
            - Normalize to lowercase (e.g. "java", "spring boot", "postgresql").
            - Maximum 20 tags.
            - If no technical skills are found, return an empty array: []
            
            Job Description:
            %s
            """;

    private final GoogleAiGeminiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public GeminiTagExtractionService(GoogleAiGeminiChatModel chatModel,
                                      ObjectMapper objectMapper) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttemptsExpression = "${jobtrackr.gemini.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${jobtrackr.gemini.retry.initial-interval-ms}",
                    multiplierExpression = "${jobtrackr.gemini.retry.multiplier}"
            )
    )
    public List<String> extractTags(String jdText) {
        if (jdText == null || jdText.isBlank()) {
            return Collections.emptyList();
        }

        log.debug("[TagExtraction] Sending JD text to Gemini");
        String prompt = EXTRACTION_PROMPT.formatted(jdText);

        // FIX: generate() returns a ChatResponse in LangChain4j 1.5.0
        // Use .aiMessage().text() to extract the text content
        String response = chatModel.chat(prompt);

        log.debug("[TagExtraction] Gemini raw response: {}", response);
        return parseTagsFromResponse(response);
    }

    @Recover
    public List<String> recoverExtractTags(Exception ex, String jdText) {
        log.error("[TagExtraction] All retries exhausted — returning empty list", ex);
        return Collections.emptyList();
    }

    private List<String> parseTagsFromResponse(String response) {
        try {
            // FIX: use replace() instead of replaceAll() for literal strings
            String cleaned = response.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            List<String> tags = objectMapper.readValue(cleaned, new TypeReference<>() {});
            log.info("[TagExtraction] Extracted {} tags", tags.size());
            return tags;
        } catch (Exception e) {
            log.error("[TagExtraction] Failed to parse Gemini response as JSON: {}", response, e);
            return Collections.emptyList();
        }
    }
}