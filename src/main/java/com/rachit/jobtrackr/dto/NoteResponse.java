package com.rachit.jobtrackr.dto;

import com.rachit.jobtrackr.entity.ApplicationNote;

import java.time.Instant;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        String content,
        Instant createdAt
) {
    public static NoteResponse from(ApplicationNote note) {
        return new NoteResponse(note.getId(), note.getContent(), note.getCreatedAt());
    }
}
