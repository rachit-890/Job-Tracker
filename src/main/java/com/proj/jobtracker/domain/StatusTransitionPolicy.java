package com.proj.jobtracker.domain;


import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Domain logic for valid status transitions.
 * Lives outside the service layer on purpose: this is a business rule
 * ("what counts as a legal move in the application pipeline?"), not
 * application plumbing. The service layer asks this class "is X -> Y legal?"
 * rather than encoding if/else chains itself.
 */
public final class StatusTransitionPolicy {

    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ApplicationStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(ApplicationStatus.APPLIED,
                EnumSet.of(ApplicationStatus.SCREENING, ApplicationStatus.REJECTED, ApplicationStatus.STALE));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.SCREENING,
                EnumSet.of(ApplicationStatus.INTERVIEW, ApplicationStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.INTERVIEW,
                EnumSet.of(ApplicationStatus.OFFER, ApplicationStatus.REJECTED));
        ALLOWED_TRANSITIONS.put(ApplicationStatus.OFFER,
                EnumSet.noneOf(ApplicationStatus.class)); // terminal
        ALLOWED_TRANSITIONS.put(ApplicationStatus.REJECTED,
                EnumSet.noneOf(ApplicationStatus.class)); // terminal
        ALLOWED_TRANSITIONS.put(ApplicationStatus.STALE,
                EnumSet.of(ApplicationStatus.SCREENING, ApplicationStatus.REJECTED)); // can resume if they respond
    }

    private StatusTransitionPolicy() {
    }

    public static boolean isValidTransition(ApplicationStatus from, ApplicationStatus to) {
        if (from == to) {
            return false;
        }
        Set<ApplicationStatus> allowed = ALLOWED_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    public static Set<ApplicationStatus> allowedNextStatuses(ApplicationStatus from) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(ApplicationStatus.class));
    }
}
