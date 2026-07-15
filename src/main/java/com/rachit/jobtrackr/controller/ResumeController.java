package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.dto.CreateResumeVersionRequest;
import com.rachit.jobtrackr.dto.ResumeVersionResponse;
import com.rachit.jobtrackr.service.ResumeVersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeVersionService resumeVersionService;

    public ResumeController(ResumeVersionService resumeVersionService) {
        this.resumeVersionService = resumeVersionService;
    }

    @PostMapping
    public ResponseEntity<ResumeVersionResponse> create(
            @Valid @RequestBody CreateResumeVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResumeVersionResponse.from(resumeVersionService.create(request)));
    }

    @GetMapping
    public List<ResumeVersionResponse> listAll() {
        return resumeVersionService.listAll().stream()
                .map(ResumeVersionResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResumeVersionResponse getById(@PathVariable UUID id) {
        return ResumeVersionResponse.from(resumeVersionService.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        resumeVersionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
