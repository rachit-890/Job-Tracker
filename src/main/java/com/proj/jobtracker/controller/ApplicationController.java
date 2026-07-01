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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@Validated // FIX: required for @Min/@Max on @RequestParam to actually fire
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        JobApplication created = applicationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApplicationResponse.from(created));
    }

    // FIX: list returns ApplicationSummaryResponse (no jdText) — use GET /{id} for full detail
    // FIX: default sort direction changed to DESC (most recent first)
    @GetMapping
    public Page<ApplicationSummaryResponse> list(
            @PageableDefault(size = 20, sort = "appliedDate", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.list(pageable).map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/search")
    public Page<ApplicationSummaryResponse> search(
            @RequestParam("q") String query,
            @PageableDefault(size = 20, sort = "appliedDate", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.search(query, pageable).map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/company/{company}")
    public Page<ApplicationSummaryResponse> byCompany(
            @PathVariable String company,
            @PageableDefault(size = 20, sort = "appliedDate", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.byCompany(company, pageable).map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/status/{status}")
    public Page<ApplicationSummaryResponse> byStatus(
            @PathVariable ApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedDate", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return applicationService.byStatus(status, pageable).map(ApplicationSummaryResponse::from);
    }

    @GetMapping("/stale")
    public Page<ApplicationSummaryResponse> stale(
            // FIX: validate bounds — 0 or negative makes the cutoff future-dated
            // and returns everything; >365 is nonsensical.
            @RequestParam(defaultValue = "14")
            @Min(value = 1, message = "staleAfterDays must be at least 1")
            @Max(value = 365, message = "staleAfterDays must be 365 or fewer")
            int staleAfterDays,
            @PageableDefault(size = 20) Pageable pageable) {
        return applicationService.stale(staleAfterDays, pageable).map(ApplicationSummaryResponse::from);
    }

    // Detail endpoint — full response including jdText
    @GetMapping("/{id}")
    public ApplicationResponse getById(@PathVariable UUID id) {
        return ApplicationResponse.from(applicationService.getById(id));
    }

    @PatchMapping("/{id}")
    public ApplicationResponse update(@PathVariable UUID id, @RequestBody UpdateApplicationRequest request) {
        return ApplicationResponse.from(applicationService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApplicationResponse updateStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
        return ApplicationResponse.from(applicationService.updateStatus(id, request.newStatus()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        applicationService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}