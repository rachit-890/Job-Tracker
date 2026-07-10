package com.rachit.jobtrackr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rachit.jobtrackr.entity.ResumeEmbedding;
import com.rachit.jobtrackr.repository.ResumeEmbeddingRepository;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class GeminiEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);

    @Value("${jobtrackr.gemini.embedding-model}")
    private String embeddingModelName;

    private final GoogleAiEmbeddingModel embeddingModel;
    private final ResumeEmbeddingRepository resumeEmbeddingRepository;
    private final ObjectMapper objectMapper;

    public GeminiEmbeddingService(GoogleAiEmbeddingModel embeddingModel,
                                  ResumeEmbeddingRepository resumeEmbeddingRepository,
                                  ObjectMapper objectMapper) {
        this.embeddingModel = embeddingModel;
        this.resumeEmbeddingRepository = resumeEmbeddingRepository;
        this.objectMapper = objectMapper;
    }

    public float[] getResumeEmbedding(String resumeText, String resumeVersion) {
        String hash = sha256(resumeText);

        return resumeEmbeddingRepository.findById(hash)
                .map(cached -> {
                    log.debug("[Embedding] Cache hit for resumeHash={}", hash);
                    return deserializeVector(cached.getEmbeddingVector());
                })
                .orElseGet(() -> {
                    log.info("[Embedding] Cache miss for resumeHash={} — calling Gemini", hash);
                    float[] vector = callGeminiEmbedding(resumeText);
                    persistResumeEmbedding(hash, resumeVersion, vector);
                    return vector;
                });
    }

    public float[] getJdEmbedding(String jdText) {
        log.debug("[Embedding] Computing JD embedding");
        return callGeminiEmbedding(jdText);
    }

    @Retryable(
            retryFor = Exception.class,
            maxAttemptsExpression = "${jobtrackr.gemini.retry.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${jobtrackr.gemini.retry.initial-interval-ms}",
                    multiplierExpression = "${jobtrackr.gemini.retry.multiplier}"
            )
    )
    public float[] callGeminiEmbedding(String text) {
        // FIX: embed() takes TextSegment, not raw String
        Response<Embedding> response = embeddingModel.embed(TextSegment.from(text));
        float[] vector = response.content().vector();
        log.debug("[Embedding] Received vector of dimension={}", vector.length);
        return vector;
    }

    @Recover
    public float[] recoverEmbedding(Exception ex, String text) {
        log.error("[Embedding] All retries exhausted — returning empty vector", ex);
        return new float[0];
    }

    private void persistResumeEmbedding(String hash, String resumeVersion, float[] vector) {
        try {
            String serialized = objectMapper.writeValueAsString(vector);
            ResumeEmbedding entity = ResumeEmbedding.builder()
                    .resumeHash(hash)
                    .resumeVersion(resumeVersion)
                    .embeddingModel(embeddingModelName)
                    .embeddingVector(serialized)
                    .generatedAt(Instant.now())
                    .build();
            resumeEmbeddingRepository.save(entity);
            log.info("[Embedding] Persisted resume embedding: hash={} version={}", hash, resumeVersion);
        } catch (Exception e) {
            log.error("[Embedding] Failed to persist resume embedding", e);
        }
    }

    private float[] deserializeVector(String json) {
        try {
            return objectMapper.readValue(json, float[].class);
        } catch (Exception e) {
            log.error("[Embedding] Failed to deserialize cached embedding vector", e);
            return new float[0];
        }
    }

    public static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}