package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.service.DltReplayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final DltReplayService dltReplayService;

    public AdminController(DltReplayService dltReplayService) {
        this.dltReplayService = dltReplayService;
    }

    @PostMapping("/dlt/replay")
    public ResponseEntity<Map<String, Object>> replayDlt(
            @RequestParam String dltTopic,
            @RequestParam String targetTopic) {
        
        int count = dltReplayService.replayDlt(dltTopic, targetTopic);
        return ResponseEntity.ok(Map.of(
                "dltTopic", dltTopic,
                "targetTopic", targetTopic,
                "replayedCount", count
        ));
    }
}
