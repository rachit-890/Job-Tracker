package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.domain.GoalPeriod;
import com.rachit.jobtrackr.dto.GoalProgressResponse;
import com.rachit.jobtrackr.service.GoalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<Void> setGoal(
            @RequestParam int targetCount,
            @RequestParam GoalPeriod period) {
        goalService.setGoal(targetCount, period);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/progress")
    public ResponseEntity<GoalProgressResponse> getProgress() {
        GoalProgressResponse progress = goalService.getProgress();
        if (progress == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(progress);
    }
}
