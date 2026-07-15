package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.domain.GoalPeriod;
import com.rachit.jobtrackr.dto.GoalProgressResponse;
import com.rachit.jobtrackr.entity.ApplicationGoal;
import com.rachit.jobtrackr.repository.ApplicationSpecification;
import com.rachit.jobtrackr.repository.GoalRepository;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final JobApplicationRepository applicationRepository;

    public GoalService(GoalRepository goalRepository, JobApplicationRepository applicationRepository) {
        this.goalRepository = goalRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public ApplicationGoal setGoal(int targetCount, GoalPeriod period) {
        goalRepository.deactivateAll();

        ApplicationGoal goal = ApplicationGoal.builder()
                .targetCount(targetCount)
                .period(period)
                .active(true)
                .build();

        return goalRepository.save(goal);
    }

    @Transactional(readOnly = true)
    public GoalProgressResponse getProgress() {
        Optional<ApplicationGoal> activeGoalOpt = goalRepository.findByActiveTrue();
        if (activeGoalOpt.isEmpty()) {
            return null; // No active goal
        }

        ApplicationGoal goal = activeGoalOpt.get();
        LocalDate start, end;
        LocalDate today = LocalDate.now();

        if (goal.getPeriod() == GoalPeriod.WEEKLY) {
            start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        } else {
            start = today.withDayOfMonth(1);
            end = today.with(TemporalAdjusters.lastDayOfMonth());
        }

        long currentCount = applicationRepository.count(
                ApplicationSpecification.notDeleted()
                        .and(ApplicationSpecification.appliedOnOrAfter(start))
                        .and(ApplicationSpecification.appliedOnOrBefore(end))
        );

        int progressPercentage = (int) Math.min(100, Math.round(((double) currentCount / goal.getTargetCount()) * 100));

        return new GoalProgressResponse(
                goal.getTargetCount(),
                goal.getPeriod(),
                currentCount,
                progressPercentage
        );
    }
}
