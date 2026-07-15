package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.dto.BulkImportResult;
import com.rachit.jobtrackr.service.CsvImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/applications/import")
public class BulkImportController {

    private final CsvImportService csvImportService;

    public BulkImportController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping
    public ResponseEntity<BulkImportResult> importCsv(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new BulkImportResult(0, 0, 0, java.util.List.of("File is empty")));
        }

        BulkImportResult result = csvImportService.importCsv(file);
        return ResponseEntity.ok(result);
    }
}
