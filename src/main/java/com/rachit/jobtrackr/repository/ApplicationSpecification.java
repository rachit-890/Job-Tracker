package com.rachit.jobtrackr.repository;


import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.entity.JobApplication;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public final class ApplicationSpecification {

    private ApplicationSpecification() {
    }

    public static Specification<JobApplication> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<JobApplication> hasStatus(ApplicationStatus status) {
        return (root, query, cb) -> cb.equal(root.get("currentStatus"), status);
    }

    public static Specification<JobApplication> companyContains(String company) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("company")), "%" + company.toLowerCase() + "%");
    }

    public static Specification<JobApplication> appliedOnOrAfter(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("appliedDate"), from);
    }

    public static Specification<JobApplication> appliedOnOrBefore(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("appliedDate"), to);
    }
}
