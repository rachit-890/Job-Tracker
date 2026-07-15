package com.rachit.jobtrackr.service;

import com.rachit.jobtrackr.domain.ApplicationStatus;
import com.rachit.jobtrackr.dto.BulkImportResult;
import com.rachit.jobtrackr.entity.JobApplication;
import com.rachit.jobtrackr.repository.JobApplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvImportService {

    private static final Logger log = LoggerFactory.getLogger(CsvImportService.class);
    private final JobApplicationRepository applicationRepository;

    public CsvImportService(JobApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    @Transactional
    public BulkImportResult importCsv(MultipartFile file) {
        int total = 0;
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();
        List<JobApplication> applicationsToSave = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new BulkImportResult(0, 0, 0, List.of("File is empty"));
            }

            // Expected headers: Company,Role,Status,AppliedDate,SourceUrl,ResumeVersion
            String line;
            int rowNum = 1; // 1 was header
            while ((line = reader.readLine()) != null) {
                rowNum++;
                total++;
                if (line.trim().isEmpty()) continue;

                String[] columns = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 0; i < columns.length; i++) {
                    columns[i] = columns[i].replaceAll("^\"|\"$", "").trim();
                }

                if (columns.length < 2) {
                    errors.add("Row " + rowNum + ": Missing required columns (Company, Role)");
                    failed++;
                    continue;
                }

                try {
                    String company = columns[0];
                    String role = columns[1];
                    if (company.isEmpty() || role.isEmpty()) {
                        throw new IllegalArgumentException("Company and Role cannot be empty");
                    }

                    ApplicationStatus status = ApplicationStatus.APPLIED;
                    if (columns.length > 2 && !columns[2].isEmpty()) {
                        status = ApplicationStatus.valueOf(columns[2].toUpperCase());
                    }

                    LocalDate appliedDate = LocalDate.now();
                    if (columns.length > 3 && !columns[3].isEmpty()) {
                        appliedDate = LocalDate.parse(columns[3]);
                    }

                    String sourceUrl = columns.length > 4 && !columns[4].isEmpty() ? columns[4] : null;
                    String resumeVersion = columns.length > 5 && !columns[5].isEmpty() ? columns[5] : "default";

                    JobApplication app = JobApplication.builder()
                            .company(company)
                            .role(role)
                            .currentStatus(status)
                            .appliedDate(appliedDate)
                            .sourceUrl(sourceUrl)
                            .resumeVersion(resumeVersion)
                            .deleted(false)
                            .build();

                    applicationsToSave.add(app);
                    success++;
                } catch (IllegalArgumentException | DateTimeParseException e) {
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                    failed++;
                }
            }

            applicationRepository.saveAll(applicationsToSave);

        } catch (Exception e) {
            log.error("Failed to parse CSV", e);
            errors.add("Critical error parsing file: " + e.getMessage());
            failed = total - success;
        }

        return new BulkImportResult(total, success, failed, errors);
    }
}
