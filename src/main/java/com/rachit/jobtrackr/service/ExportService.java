package com.rachit.jobtrackr.service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Streaming export service for CSV and PDF.
 *
 * Why streaming?
 * Loading all applications into memory before writing would allocate a large
 * heap buffer for users with hundreds of applications. Streaming writes each
 * row directly to the response OutputStream as it's fetched, keeping memory
 * consumption constant regardless of dataset size.
 *
 * CSV uses PrintWriter directly to the response stream.
 * PDF uses iText's Document + PdfWriter piped to the response OutputStream.
 */
@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private static final int BATCH_SIZE = 100;

    private final JobApplicationRepository applicationRepository;

    public ExportService(JobApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    /**
     * Streams all non-deleted applications as CSV to the given OutputStream.
     * Writes in batches to keep memory constant.
     */
    public void exportCsv(OutputStream outputStream) throws IOException {
        PrintWriter writer = new PrintWriter(outputStream);

        // Header row
        writer.println("Company,Role,Status,Applied Date,Resume Version,Match Score,Source URL");

        int page = 0;
        List<JobApplication> batch;

        do {
            batch = applicationRepository.findAllByDeletedFalse(
                    PageRequest.of(page, BATCH_SIZE,
                            Sort.by(Sort.Direction.DESC, "appliedDate"))
            ).getContent();

            for (JobApplication app : batch) {
                writer.println(String.join(",",
                        escapeCsv(app.getCompany()),
                        escapeCsv(app.getRole()),
                        app.getCurrentStatus().name(),
                        app.getAppliedDate().toString(),
                        escapeCsv(app.getResumeVersion()),
                        app.getMatchScore() != null ? app.getMatchScore().toString() : "",
                        escapeCsv(app.getSourceUrl())
                ));
            }

            writer.flush();
            page++;

        } while (!batch.isEmpty() && batch.size() == BATCH_SIZE);

        log.info("[Export] CSV export completed: {} pages", page);
    }

    /**
     * Streams all non-deleted applications as PDF to the given OutputStream.
     */
    public void exportPdf(OutputStream outputStream) throws IOException {
        Document document = new Document();

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            document.add(new Paragraph("JobTrackr — Application History", titleFont));
            document.add(new Paragraph("Exported: " + java.time.LocalDate.now()));
            document.add(new Paragraph(" "));

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font bodyFont  = FontFactory.getFont(FontFactory.HELVETICA, 10);

            int page = 0;
            List<JobApplication> batch;

            do {
                batch = applicationRepository.findAllByDeletedFalse(
                        PageRequest.of(page, BATCH_SIZE,
                                Sort.by(Sort.Direction.DESC, "appliedDate"))
                ).getContent();

                for (JobApplication app : batch) {
                    document.add(new Paragraph(
                            app.getCompany() + " — " + app.getRole(), headerFont));
                    document.add(new Paragraph(
                            "Status: " + app.getCurrentStatus() +
                                    "  |  Applied: " + app.getAppliedDate() +
                                    (app.getResumeVersion() != null
                                            ? "  |  Resume: " + app.getResumeVersion() : "") +
                                    (app.getMatchScore() != null
                                            ? "  |  Match: " + app.getMatchScore() + "%" : ""),
                            bodyFont));
                    document.add(new Paragraph(" "));
                }

                page++;

            } while (!batch.isEmpty() && batch.size() == BATCH_SIZE);

            log.info("[Export] PDF export completed: {} pages", page);

        } catch (DocumentException e) {
            throw new IOException("Failed to generate PDF", e);
        } finally {
            document.close();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        // Wrap in quotes and escape internal quotes
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}