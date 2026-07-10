package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.entity.ApplicationTag;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.entity.TagSource;
import com.rachit.jobtrackr.event.ApplicationCreatedPayload;
import com.rachit.jobtrackr.event.ResumeScoredPayload;
import com.rachit.jobtrackr.exception.ResourceNotFoundException;
import com.rachit.jobtrackr.repository.ApplicationTagRepository;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates AI processing for a new application:
 * 1. Extract JD tags via Gemini chat
 * 2. Save tags to application_tags with source = AI
 * 3. Compute resume embedding (cache-first)
 * 4. Compute JD embedding
 * 5. Calculate cosine similarity → match score
 * 6. Update applications.match_score
 * 7. Publish ResumeScoredEvent
 *
 * This class is called by AiConsumer and is intentionally separate from
 * ApplicationService to keep AI concerns isolated from core CRUD logic.
 */
@Service
public class AiProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AiProcessingService.class);

    private final GeminiTagExtractionService tagExtractionService;
    private final GeminiEmbeddingService embeddingService;
    private final MatchScoreService matchScoreService;
    private final ApplicationTagRepository tagRepository;
    private final JobApplicationRepository applicationRepository;
    private final EventPublisher eventPublisher;

    public AiProcessingService(GeminiTagExtractionService tagExtractionService,
                               GeminiEmbeddingService embeddingService,
                               MatchScoreService matchScoreService,
                               ApplicationTagRepository tagRepository,
                               JobApplicationRepository applicationRepository,
                               EventPublisher eventPublisher) {
        this.tagExtractionService = tagExtractionService;
        this.embeddingService = embeddingService;
        this.matchScoreService = matchScoreService;
        this.tagRepository = tagRepository;
        this.applicationRepository = applicationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void processApplication(ApplicationCreatedPayload payload) {
        log.info("[AI Processing] Starting for applicationId={}", payload.applicationId());

        // Step 1 & 2: Extract JD tags and save them
        if (payload.jdText() != null && !payload.jdText().isBlank()) {
            extractAndSaveTags(payload);
        } else {
            log.debug("[AI Processing] No jdText — skipping tag extraction");
        }

        // Step 3–7: Compute embeddings and match score
        if (payload.resumeText() != null && !payload.resumeText().isBlank()
                && payload.jdText() != null && !payload.jdText().isBlank()) {
            computeAndSaveMatchScore(payload);
        } else {
            log.debug("[AI Processing] Missing resumeText or jdText — skipping match score");
        }

        log.info("[AI Processing] Completed for applicationId={}", payload.applicationId());
    }

    private void extractAndSaveTags(ApplicationCreatedPayload payload) {
        log.debug("[AI Processing] Extracting tags for applicationId={}", payload.applicationId());

        List<String> tags = tagExtractionService.extractTags(payload.jdText());

        if (tags.isEmpty()) {
            log.debug("[AI Processing] No tags extracted");
            return;
        }

        // Delete any existing AI tags in case this is a reprocessing run
        tagRepository.findByApplicationId(payload.applicationId())
                .stream()
                .filter(t -> t.getSource() == TagSource.AI)
                .forEach(t -> tagRepository.deleteById(t.getId()));

        List<ApplicationTag> tagEntities = tags.stream()
                .map(tag -> ApplicationTag.builder()
                        .applicationId(payload.applicationId())
                        .tag(tag)
                        .source(TagSource.AI)
                        .build())
                .toList();

        tagRepository.saveAll(tagEntities);
        log.info("[AI Processing] Saved {} AI tags for applicationId={}",
                tags.size(), payload.applicationId());
    }

    private void computeAndSaveMatchScore(ApplicationCreatedPayload payload) {
        log.debug("[AI Processing] Computing match score for applicationId={}",
                payload.applicationId());

        // Resume embedding — cache-first (keyed by content hash)
        float[] resumeVector = embeddingService.getResumeEmbedding(
                payload.resumeText(), payload.resumeVersion());

        // JD embedding — always computed fresh (unique per application)
        float[] jdVector = embeddingService.getJdEmbedding(payload.jdText());

        Double matchScore = matchScoreService.computeMatchScore(resumeVector, jdVector);

        if (matchScore == null) {
            log.warn("[AI Processing] Match score could not be computed for applicationId={}",
                    payload.applicationId());
            return;
        }

        // Update the application row with the computed score
        JobApplication application = applicationRepository
                .findByIdAndDeletedFalse(payload.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found during AI processing: " + payload.applicationId()));

        application.setMatchScore(matchScore);
        applicationRepository.save(application);
        log.info("[AI Processing] Match score {} saved for applicationId={}",
                matchScore, payload.applicationId());

        // Notify downstream that scoring is complete
        eventPublisher.publishResumeScored(new ResumeScoredPayload(
                payload.applicationId(),
                matchScore,
                payload.resumeVersion()
        ));
    }
}