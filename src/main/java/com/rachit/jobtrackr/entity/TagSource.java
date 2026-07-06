package com.rachit.jobtrackr.entity;

/**
 * Tracks how a tag was added to an application.
 * MANUAL  — added directly by the user via the API.
 * AI      — extracted automatically by Gemini from the JD text (Phase 4).
 */
public enum TagSource {
    MANUAL,
    AI
}