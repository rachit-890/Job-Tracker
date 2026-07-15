package com.rachit.jobtrackr.dto;

/**
 * Represents a single link in the Sankey status-flow diagram.
 * Each link connects two statuses and carries the count of applications
 * that transitioned from source to target.
 */
public record StatusFlowLink(
        String source,
        String target,
        long value
) {
}
