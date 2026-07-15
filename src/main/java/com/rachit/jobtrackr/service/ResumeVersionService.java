package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.dto.CreateResumeVersionRequest;
import com.rachit.jobtrackr.entity.ResumeVersion;
import com.rachit.jobtrackr.exception.ResourceNotFoundException;
import com.rachit.jobtrackr.repository.ResumeVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ResumeVersionService {

    private final ResumeVersionRepository resumeVersionRepository;

    public ResumeVersionService(ResumeVersionRepository resumeVersionRepository) {
        this.resumeVersionRepository = resumeVersionRepository;
    }

    @Transactional
    public ResumeVersion create(CreateResumeVersionRequest request) {
        if (resumeVersionRepository.findByLabel(request.label()).isPresent()) {
            throw new IllegalArgumentException("Resume version with label '" + request.label() + "' already exists");
        }

        ResumeVersion version = ResumeVersion.builder()
                .label(request.label())
                .content(request.content())
                .build();

        return resumeVersionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public List<ResumeVersion> listAll() {
        return resumeVersionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public ResumeVersion getById(UUID id) {
        return resumeVersionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resume version not found: " + id));
    }

    @Transactional(readOnly = true)
    public ResumeVersion getByLabel(String label) {
        return resumeVersionRepository.findByLabel(label)
                .orElseThrow(() -> new ResourceNotFoundException("Resume version not found: " + label));
    }

    @Transactional
    public void delete(UUID id) {
        if (!resumeVersionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resume version not found: " + id);
        }
        resumeVersionRepository.deleteById(id);
    }
}
