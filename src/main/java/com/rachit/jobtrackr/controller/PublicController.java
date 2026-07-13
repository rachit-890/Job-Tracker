package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.dto.AnalyticsSummaryResponse;
import com.rachit.jobtrackr.service.AnalyticsQueryService;
import com.rachit.jobtrackr.service.ShareTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read-only analytics endpoint.
 * Requires a valid, non-expired, non-revoked share token.
 * Returns only aggregate stats — no company names, JD text, or personal data.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicController {

    private final ShareTokenService shareTokenService;
    private final AnalyticsQueryService analyticsQueryService;

    public PublicController(ShareTokenService shareTokenService,
                            AnalyticsQueryService analyticsQueryService) {
        this.shareTokenService = shareTokenService;
        this.analyticsQueryService = analyticsQueryService;
    }

    @GetMapping("/{token}/analytics")
    public PublicAnalyticsResponse getPublicAnalytics(@PathVariable String token) {
        // Validates token is non-revoked and non-expired — throws 404 if not
        shareTokenService.validateAndGet(token);

        AnalyticsSummaryResponse summary = analyticsQueryService.getSummary();

        // Return only anonymized aggregate data — no company names or JD text
        return new PublicAnalyticsResponse(
                summary.totalApplications(),
                summary.responseRate(),
                summary.appliedCount(),
                summary.screeningCount(),
                summary.interviewCount(),
                summary.offerCount(),
                summary.rejectedCount(),
                summary.staleCount()
        );
    }

    public record PublicAnalyticsResponse(
            int totalApplications,
            double responseRate,
            int appliedCount,
            int screeningCount,
            int interviewCount,
            int offerCount,
            int rejectedCount,
            int staleCount
    ) {}
}