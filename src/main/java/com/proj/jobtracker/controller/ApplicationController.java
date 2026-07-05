package com.proj.jobtracker.controller;

import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.dto.ApplicationDetailResponse;
import com.proj.jobtracker.dto.ApplicationSummaryResponse;
import com.proj.jobtracker.dto.CreateApplicationRequest;
import com.proj.jobtracker.dto.CreateShareTokenResponse;
import com.proj.jobtracker.dto.StatusUpdateRequest;
import com.proj.jobtracker.dto.UpdateApplicationRequest;
import com.proj.jobtracker.entity.JobApplication;
import com.proj.jobtracker.entity.ShareToken;
import com.proj.jobtracker.service.ApplicationService;
import com.proj.jobtracker.service.ShareTokenService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@Validated
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ShareTokenService shareTokenService;

    @Value("${jobtrackr.public.base-url:http://localhost:8080}")
    private String baseUrl;

    public ApplicationController(ApplicationService applicationService,
                                  ShareTokenService shareTokenService) {
        this.applicationService = applicationService;
        this.shareTokenService = shareTokenService;
    }

    @PostMapping
    public ResponseEntity<ApplicationDetailResponse> create(
            @Valid @RequestBody CreateApplicationRequest request) {
        JobApplication created = applicationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.getDetail(created.getId()));
    }

    // All filters optional and composable:
    // GET /api/v1/applications
    // GET /api/v1/applications?status=APPLIED
    // GET /api/v1/applications?company=google
    // GET /api/v1/applications?appliedDateFrom=2026-01-01&appliedDateTo=2026-06-30
    // Any combination of the above
    @GetMapping
    public Page<ApplicationSummaryResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedDateFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedDateTo,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService
                .list(status, company, appliedDateFrom, appliedDateTo, pageable)
                .map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/search")
    public Page<ApplicationSummaryResponse> search(
            @RequestParam("q") String query,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.search(query, pageable)
                .map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/company/{company}")
    public Page<ApplicationSummaryResponse> byCompany(
            @PathVariable String company,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.byCompany(company, pageable)
                .map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/status/{status}")
    public Page<ApplicationSummaryResponse> byStatus(
            @PathVariable ApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.byStatus(status, pageable)
                .map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/stale")
    public Page<ApplicationSummaryResponse> stale(
            @RequestParam(defaultValue = "14")
            @Min(value = 1, message = "staleAfterDays must be at least 1")
            @Max(value = 365, message = "staleAfterDays must be 365 or fewer")
            int staleAfterDays,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.stale(staleAfterDays, pageable)
                .map(ApplicationSummaryResponse::from);
    }

    // Full detail — includes jdText, status history, AI-extracted tags
    @GetMapping("/{id}")
    public ApplicationDetailResponse getById(@PathVariable UUID id) {
        return applicationService.getDetail(id);
    }

    @PatchMapping("/{id}")
    public ApplicationDetailResponse update(
            @PathVariable UUID id,
            @RequestBody UpdateApplicationRequest request) {
        applicationService.update(id, request);
        return applicationService.getDetail(id);
    }

    @PatchMapping("/{id}/status")
    public ApplicationDetailResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request) {
        applicationService.updateStatus(id, request.newStatus());
        return applicationService.getDetail(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        applicationService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/share-token")
    public ResponseEntity<CreateShareTokenResponse> generateShareToken() {
        ShareToken token = shareTokenService.generate();
        String shareUrl = baseUrl + "/api/v1/public/" + token.getToken() + "/analytics";
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CreateShareTokenResponse(
                        token.getId(),
                        token.getToken(),
                        token.getExpiresAt(),
                        shareUrl));
    }

    @DeleteMapping("/share-token/{token}")
    public ResponseEntity<Void> revokeShareToken(@PathVariable String token) {
        shareTokenService.revoke(token);
        return ResponseEntity.noContent().build();
    }
}