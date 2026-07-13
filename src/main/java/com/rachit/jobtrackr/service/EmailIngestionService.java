package com.rachit.jobtrackr.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.dto.EmailIngestRequest;
import com.rachit.jobtrackr.dto.EmailIngestResponse;
import com.rachit.jobtrackr.entity.EmailIngestionLog;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.repository.EmailIngestionLogRepository;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Idempotent email ingestion pipeline:
 * 1. Compute idempotency key (Message-ID or SHA-256 of content)
 * 2. Check if already processed — if so, return DUPLICATE immediately
 * 3. Classify email via Gemini (REJECTION / INTERVIEW / OTHER)
 * 4. Match to an existing application by company + role hints
 * 5. If matched: trigger status transition via ApplicationService
 * 6. Persist ingestion log (idempotency record)
 */
@Service
public class EmailIngestionService {

    private static final Logger log = LoggerFactory.getLogger(EmailIngestionService.class);

    private static final String CLASSIFICATION_PROMPT = """
            Classify this job application email into exactly one of these categories:
            - REJECTION: The company is rejecting the candidate
            - INTERVIEW: The company is inviting the candidate for an interview or next step
            - OTHER: Anything else (acknowledgment, follow-up, etc.)
            
            Return ONLY a valid JSON object with this exact structure, no explanation:
            {"classification": "REJECTION|INTERVIEW|OTHER", "company": "company name if mentioned", "role": "role name if mentioned"}
            
            Email content:
            %s
            """;

    private final EmailIngestionLogRepository ingestionLogRepository;
    private final JobApplicationRepository applicationRepository;
    private final ApplicationService applicationService;
    private final EventPublisher eventPublisher;
    private final GoogleAiGeminiChatModel chatModel;
    private final ObjectMapper objectMapper;

    public EmailIngestionService(EmailIngestionLogRepository ingestionLogRepository,
                                 JobApplicationRepository applicationRepository,
                                 ApplicationService applicationService,
                                 EventPublisher eventPublisher,
                                 GoogleAiGeminiChatModel chatModel,
                                 ObjectMapper objectMapper) {
        this.ingestionLogRepository = ingestionLogRepository;
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.eventPublisher = eventPublisher;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EmailIngestResponse ingest(EmailIngestRequest request) {
        String idempotencyKey = computeIdempotencyKey(request);

        // Step 1: Check for duplicate
        if (ingestionLogRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.info("[EmailIngestion] Duplicate detected, skipping: key={}", idempotencyKey);
            return new EmailIngestResponse("DUPLICATE", null, null,
                    "Email already processed");
        }

        // Step 2: Classify via Gemini
        Map<String, String> classification = classifyEmail(request.emailContent());
        String classificationResult = classification.getOrDefault("classification", "OTHER");
        String detectedCompany = classification.getOrDefault("company",
                request.companyHint() != null ? request.companyHint() : "");
        String detectedRole = classification.getOrDefault("role",
                request.roleHint() != null ? request.roleHint() : "");

        log.info("[EmailIngestion] Classified as {} for company='{}' role='{}'",
                classificationResult, detectedCompany, detectedRole);

        // Step 3: Match to an application
        Optional<JobApplication> matchedApp = findMatchingApplication(
                detectedCompany, detectedRole);

        // Step 4: Trigger status transition if matched
        if (matchedApp.isPresent() && !classificationResult.equals("OTHER")) {
            JobApplication app = matchedApp.get();
            ApplicationStatus newStatus = classificationResult.equals("REJECTION")
                    ? ApplicationStatus.REJECTED
                    : ApplicationStatus.SCREENING;

            try {
                applicationService.updateStatus(app.getId(), newStatus);
                log.info("[EmailIngestion] Status updated: applicationId={} -> {}",
                        app.getId(), newStatus);
            } catch (Exception e) {
                log.warn("[EmailIngestion] Could not update status for applicationId={}: {}",
                        app.getId(), e.getMessage());
            }
        }

        // Step 5: Record ingestion log
        EmailIngestionLog logEntry = EmailIngestionLog.builder()
                .idempotencyKey(idempotencyKey)
                .applicationId(matchedApp.map(JobApplication::getId).orElse(null))
                .classification(classificationResult)
                .processedAt(Instant.now())
                .build();
        ingestionLogRepository.save(logEntry);

        String status = matchedApp.isPresent() ? "PROCESSED" : "NO_MATCH";
        String message = matchedApp.isPresent()
                ? "Matched to application and status updated"
                : "Classified but no matching application found";

        return new EmailIngestResponse(
                status,
                classificationResult,
                matchedApp.map(JobApplication::getId).orElse(null),
                message
        );
    }

    private Map<String, String> classifyEmail(String emailContent) {
        try {
            String prompt = CLASSIFICATION_PROMPT.formatted(
                    emailContent.substring(0, Math.min(emailContent.length(), 3000)));
            String response = chatModel.chat(prompt);
            String cleaned = response.trim()
                    .replace("```json", "").replace("```", "").trim();
            return objectMapper.readValue(cleaned, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("[EmailIngestion] Failed to classify email", e);
            return Map.of("classification", "OTHER");
        }
    }

    private Optional<JobApplication> findMatchingApplication(String company, String role) {
        if (company == null || company.isBlank()) return Optional.empty();

        Page<JobApplication> candidates = applicationRepository
                .findByCompanyIgnoreCaseAndDeletedFalse(company.trim(),
                        PageRequest.of(0, 5));

        if (candidates.isEmpty()) return Optional.empty();

        // If role hint available, try to narrow down
        if (role != null && !role.isBlank()) {
            String roleLower = role.toLowerCase();
            return candidates.getContent().stream()
                    .filter(a -> a.getRole().toLowerCase().contains(roleLower))
                    .findFirst()
                    .or(() -> candidates.getContent().stream().findFirst());
        }

        return candidates.getContent().stream().findFirst();
    }

    private String computeIdempotencyKey(EmailIngestRequest request) {
        // Use Message-ID header if present (most reliable dedup key)
        if (request.messageId() != null && !request.messageId().isBlank()) {
            return sha256(request.messageId().trim());
        }
        // Fall back to SHA-256 of normalized content
        String normalized = request.emailContent()
                .replaceAll("\\s+", " ").trim().toLowerCase();
        return sha256(normalized);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}