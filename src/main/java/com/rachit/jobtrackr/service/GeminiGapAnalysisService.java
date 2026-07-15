package com.rachit.jobtrackr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rachit.jobtrackr.dto.GapAnalysisResult;
import com.rachit.jobtrackr.metrics.MetricsService;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Uses Gemini to generate a structured gap analysis comparing a resume
 * to a job description.
 *
 * Rate limit mitigation (addressing user concern about 3 Gemini calls):
 * This runs ONLY when both resumeText and jdText are provided AND a match
 * score was successfully computed. If either is missing, this is skipped.
 * Combined with @Retryable backoff, we stay well within typical Gemini
 * rate limits (~60 RPM for free tier, 1000+ RPM for paid).
 */
@Service
public class GeminiGapAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(GeminiGapAnalysisService.class);

    private static final String GAP_ANALYSIS_PROMPT = """
            You are a career advisor. Compare the candidate's resume against the job description
            and produce a structured gap analysis.
            
            Rules:
            - Return ONLY valid JSON with this exact structure, no markdown, no code block:
              {"resumeCovers": [...], "resumeLacks": [...], "summary": "..."}
            - resumeCovers: array of skills/technologies the resume demonstrates that match the JD
            - resumeLacks: array of skills/technologies required by the JD but missing from the resume
            - summary: 2-3 sentence plain English summary of the overall gap
            - Normalize skill names to lowercase (e.g. "java", "kubernetes", "system design")
            - Maximum 15 items in each array
            - If the resume or JD is too short to analyze, return empty arrays and a summary saying so
            
            Resume:
            %s
            
            Job Description:
            %s
            """;

    private final GoogleAiGeminiChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final MetricsService metricsService;

    public GeminiGapAnalysisService(GoogleAiGeminiChatModel chatModel,
                                     ObjectMapper objectMapper,
                                     MetricsService metricsService) {
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.metricsService = metricsService;
    }

    @Retryable(
            retryFor = Exception.class,
            noRetryFor = {dev.langchain4j.exception.RateLimitException.class},
            maxAttemptsExpression = "${jobtrackr.gemini.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${jobtrackr.gemini.retry.initial-interval-ms}",
                    multiplierExpression = "${jobtrackr.gemini.retry.multiplier}"
            )
    )
    public GapAnalysisResult analyze(String resumeText, String jdText) {
        if (resumeText == null || resumeText.isBlank()
                || jdText == null || jdText.isBlank()) {
            return null;
        }

        log.debug("[GapAnalysis] Sending resume + JD to Gemini");
        long start = System.currentTimeMillis();
        String prompt = GAP_ANALYSIS_PROMPT.formatted(resumeText, jdText);
        String response = chatModel.chat(prompt);
        metricsService.recordGeminiChatCall(System.currentTimeMillis() - start);

        log.debug("[GapAnalysis] Gemini raw response: {}", response);
        return parseResponse(response);
    }

    @Recover
    public GapAnalysisResult recoverAnalyze(Exception ex, String resumeText, String jdText) {
        log.error("[GapAnalysis] All retries exhausted — returning null", ex);
        return null;
    }

    private GapAnalysisResult parseResponse(String response) {
        try {
            String cleaned = response.trim()
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
            GapAnalysisResult result = objectMapper.readValue(cleaned, GapAnalysisResult.class);
            log.info("[GapAnalysis] Parsed: covers={} lacks={}",
                    result.resumeCovers().size(), result.resumeLacks().size());
            return result;
        } catch (Exception e) {
            log.error("[GapAnalysis] Failed to parse Gemini response: {}", response, e);
            return new GapAnalysisResult(Collections.emptyList(), Collections.emptyList(),
                    "Gap analysis could not be generated");
        }
    }
}
