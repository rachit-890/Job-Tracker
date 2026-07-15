package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.dto.CreateNoteRequest;
import com.rachit.jobtrackr.dto.NoteResponse;
import com.rachit.jobtrackr.entity.ApplicationNote;
import com.rachit.jobtrackr.service.NoteService;
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
@RequestMapping("/api/v1/applications/{applicationId}/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    public ResponseEntity<NoteResponse> addNote(
            @PathVariable UUID applicationId,
            @Valid @RequestBody CreateNoteRequest request) {
        ApplicationNote note = noteService.addNote(applicationId, request.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.from(note));
    }

    @GetMapping
    public List<NoteResponse> getNotes(@PathVariable UUID applicationId) {
        return noteService.getNotes(applicationId).stream()
                .map(NoteResponse::from)
                .toList();
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable UUID applicationId,
            @PathVariable UUID noteId) {
        noteService.deleteNote(applicationId, noteId);
        return ResponseEntity.noContent().build();
    }
}
