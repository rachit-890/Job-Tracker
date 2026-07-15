package com.rachit.jobtrackr.dto;

public record BulkImportResult(
        int totalRows,
        int successfulImports,
        int failedImports,
        java.util.List<String> errors
) {}
