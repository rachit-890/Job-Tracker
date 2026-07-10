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

        if (payload.jdText() != null && !payload.jdText().isBlank()) {
            extractAndSaveTags(payload);
        } else {
            log.debug("[AI Processing] No jdText — skipping tag extraction");
        }

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

        tagRepository.deleteByApplicationIdAndSource(payload.applicationId(), TagSource.AI);

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

        float[] resumeVector = embeddingService.getResumeEmbedding(
                payload.resumeText(), payload.resumeVersion());

        float[] jdVector = embeddingService.getJdEmbedding(payload.jdText());

        Double matchScore = matchScoreService.computeMatchScore(resumeVector, jdVector);

        if (matchScore == null) {
            log.warn("[AI Processing] Match score could not be computed for applicationId={}",
                    payload.applicationId());
            return;
        }

        JobApplication application = applicationRepository
                .findByIdAndDeletedFalse(payload.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found during AI processing: "
                                + payload.applicationId()));

        application.setMatchScore(matchScore);
        applicationRepository.save(application);
        log.info("[AI Processing] Match score {} saved for applicationId={}",
                matchScore, payload.applicationId());

        eventPublisher.publishResumeScored(new ResumeScoredPayload(
                payload.applicationId(),
                matchScore,
                payload.resumeVersion()
        ));
    }
}