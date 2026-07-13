package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.dto.CaptureRequest;
import com.rachit.jobtrackr.dto.CreateApplicationRequest;
import com.rachit.jobtrackr.dto.EmailIngestRequest;
import com.rachit.jobtrackr.dto.EmailIngestResponse;
import com.rachit.jobtrackr.service.ApplicationService;
import com.rachit.jobtrackr.service.EmailIngestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class IngestController {

    private static final Logger log = LoggerFactory.getLogger(IngestController.class);

    private final EmailIngestionService emailIngestionService;
    private final ApplicationService applicationService;

    public IngestController(EmailIngestionService emailIngestionService,
                            ApplicationService applicationService) {
        this.emailIngestionService = emailIngestionService;
        this.applicationService = applicationService;
    }

    /**
     * Idempotent email ingestion.
     * Safe to call multiple times with the same email — duplicate detection
     * via SHA-256 content hash (or Message-ID header if present).
     */
    @PostMapping("/email-ingest")
    public ResponseEntity<EmailIngestResponse> ingestEmail(
            @Valid @RequestBody EmailIngestRequest request) {
        EmailIngestResponse response = emailIngestionService.ingest(request);
        log.info("[Ingest] Email ingested: status={} classification={}",
                response.status(), response.classification());
        return ResponseEntity.ok(response);
    }

    /**
     * Browser-extension quick-add.
     * Public endpoint — no auth required.
     * Rate-limited via Redis in SecurityConfig (Phase 8 hardening).
     * Creates a minimal application record with just company, role, sourceUrl.
     */
    @PostMapping("/capture")
    public ResponseEntity<Void> capture(@Valid @RequestBody CaptureRequest request) {
        CreateApplicationRequest appRequest = new CreateApplicationRequest(
                request.company(),
                request.role(),
                null,  // jdText
                null,  // resumeVersion
                null,  // resumeText
                request.sourceUrl(),
                request.appliedDate() != null ? request.appliedDate() : LocalDate.now()
        );

        applicationService.create(appRequest);
        log.info("[Capture] Quick-add: company={} role={}", request.company(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}