package com.proj.jobtracker.controller;

import com.proj.jobtracker.domain.ApplicationStatus;
import com.proj.jobtracker.dto.*;
import com.proj.jobtracker.entity.JobApplication;
import com.proj.jobtracker.service.ApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@Validated
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationDetailResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        JobApplication created = applicationService.create(request);
        // Return detail (with empty history/tags) so the client sees the full
        // structure immediately rather than having to call GET /{id} separately.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(applicationService.getDetail(created.getId()));
    }

    // List — summary only (no jdText), newest first.
    // Optional date-range: both params nullable, open-ended ranges supported.
    @GetMapping
    public Page<ApplicationSummaryResponse> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedAfter,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedBefore,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {

        if (appliedAfter != null || appliedBefore != null) {
            return applicationService
                    .listByDateRange(appliedAfter, appliedBefore, pageable)
                    .map(ApplicationSummaryResponse::from);
        }
        return applicationService.list(pageable).map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/search")
    public Page<ApplicationSummaryResponse> search(
            @RequestParam("q") String query,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.search(query, pageable).map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/company/{company}")
    public Page<ApplicationSummaryResponse> byCompany(
            @PathVariable String company,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.byCompany(company, pageable).map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/status/{status}")
    public Page<ApplicationSummaryResponse> byStatus(
            @PathVariable ApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.byStatus(status, pageable).map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/stale")
    public Page<ApplicationSummaryResponse> stale(
            @RequestParam(defaultValue = "14")
            @Min(value = 1,   message = "staleAfterDays must be at least 1")
            @Max(value = 365, message = "staleAfterDays must be 365 or fewer")
            int staleAfterDays,
            @PageableDefault(size = 20) Pageable pageable) {
        return applicationService.stale(staleAfterDays, pageable).map(ApplicationSummaryResponse::from);
    }

    // Full detail — includes jdText, status history, tags
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

    // ── Tag endpoints ────────────────────────────────────────────────────────

    @GetMapping("/{id}/tags")
    public List<String> getTags(@PathVariable UUID id) {
        return applicationService.getTagsForApplication(id);
    }

    @PostMapping("/{id}/tags")
    public ResponseEntity<List<String>> addTag(
            @PathVariable UUID id,
            @Valid @RequestBody AddTagRequest request) {
        List<String> tags = applicationService.addTag(id, request.tag());
        return ResponseEntity.status(HttpStatus.CREATED).body(tags);
    }

    @DeleteMapping("/{id}/tags/{tag}")
    public List<String> removeTag(@PathVariable UUID id, @PathVariable String tag) {
        return applicationService.removeTag(id, tag);
    }
}