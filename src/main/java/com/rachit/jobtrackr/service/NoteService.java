package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.entity.ApplicationNote;
import com.rachit.jobtrackr.exception.ResourceNotFoundException;
import com.rachit.jobtrackr.repository.ApplicationNoteRepository;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NoteService {

    private final ApplicationNoteRepository noteRepository;
    private final JobApplicationRepository applicationRepository;

    public NoteService(ApplicationNoteRepository noteRepository,
                       JobApplicationRepository applicationRepository) {
        this.noteRepository = noteRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ApplicationNote addNote(UUID applicationId, String content) {
        // Verify the application exists
        applicationRepository.findByIdAndDeletedFalse(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application not found: " + applicationId));

        ApplicationNote note = ApplicationNote.builder()
                .applicationId(applicationId)
                .content(content)
                .build();

        return noteRepository.save(note);
    }

    @Transactional(readOnly = true)
    public List<ApplicationNote> getNotes(UUID applicationId) {
        return noteRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId);
    }

    @Transactional
    public void deleteNote(UUID applicationId, UUID noteId) {
        ApplicationNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Note not found: " + noteId));

        if (!note.getApplicationId().equals(applicationId)) {
            throw new ResourceNotFoundException("Note does not belong to application: " + applicationId);
        }

        noteRepository.delete(note);
    }
}
