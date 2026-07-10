package com.rachit.jobtrackr.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Computes a resume-to-JD match score using cosine similarity.
 *
 * Cosine similarity measures the angle between two vectors in high-dimensional space.
 * A value of 1.0 means the texts are identical in meaning, 0.0 means orthogonal,
 * -1.0 means opposite (rare for text).
 *
 * For Gemini's text-embedding-004 model, similarity between semantically related
 * texts is typically 0.7–0.9. We normalize to 0–100 for a friendlier score.
 *
 * Why not Euclidean distance?
 * Cosine similarity is length-invariant — a longer resume won't be penalized
 * just for having more text. It measures semantic direction, not magnitude,
 * which is exactly what we want for resume-to-JD matching.
 */
@Service
public class MatchScoreService {

    private static final Logger log = LoggerFactory.getLogger(MatchScoreService.class);

    /**
     * Returns a match score between 0 and 100.
     * Returns null if either vector is empty (Gemini call failed).
     */
    public Double computeMatchScore(float[] resumeVector, float[] jdVector) {
        if (resumeVector == null || resumeVector.length == 0
                || jdVector == null || jdVector.length == 0) {
            log.warn("[MatchScore] One or both vectors are empty — skipping match score");
            return null;
        }

        if (resumeVector.length != jdVector.length) {
            log.error("[MatchScore] Vector dimension mismatch: resume={} jd={}",
                    resumeVector.length, jdVector.length);
            return null;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < resumeVector.length; i++) {
            dotProduct += resumeVector[i] * jdVector[i];
            normA += resumeVector[i] * resumeVector[i];
            normB += jdVector[i] * jdVector[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        double cosineSimilarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));

        // Gemini embeddings are normalized — cosine similarity is between 0 and 1.
        // Clamp to [0, 1] to handle any floating-point edge cases, then scale to 0–100.
        double clamped = Math.max(0.0, Math.min(1.0, cosineSimilarity));
        double score = Math.round(clamped * 100.0 * 100.0) / 100.0; // 2 decimal places

        log.info("[MatchScore] Computed match score: cosineSimilarity={} score={}",
                String.format("%.4f", cosineSimilarity), score);

        return score;
    }
}