package com.rachit.jobtrackr.controller;

import com.rachit.jobtrackr.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/export")
public class ExportController {

    private static final Logger log = LoggerFactory.getLogger(ExportController.class);

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"jobtrackr-applications.csv\"");
        exportService.exportCsv(response.getOutputStream());
        log.info("[Export] CSV download served");
    }

    @GetMapping("/pdf")
    public void exportPdf(HttpServletResponse response) throws IOException {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"jobtrackr-applications.pdf\"");
        exportService.exportPdf(response.getOutputStream());
        log.info("[Export] PDF download served");
    }
}