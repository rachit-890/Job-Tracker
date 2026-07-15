package com.rachit.jobtrackr.dto;

import java.util.List;

/**
 * Structured gap analysis result from Gemini.
 * Stored as JSONB on the applications table.
 *
 * resumeCovers: skills/technologies the resume demonstrates
 * resumeLacks:  skills/technologies required by the JD but missing from resume
 * summary:      2-3 sentence plain-English summary of the gap
 */
public record GapAnalysisResult(
        List<String> resumeCovers,
        List<String> resumeLacks,
        String summary
) {}
