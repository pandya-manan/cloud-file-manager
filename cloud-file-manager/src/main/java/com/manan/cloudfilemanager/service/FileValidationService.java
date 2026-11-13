
package com.manan.cloudfilemanager.service;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES = Map.ofEntries(
            Map.entry(".pdf", Set.of("application/pdf")),
            Map.entry(".doc", Set.of("application/msword")),
            Map.entry(".docx", Set.of(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry(".txt", Set.of("text/plain")),
            Map.entry(".xls", Set.of("application/vnd.ms-excel")),
            Map.entry(".xlsx", Set.of(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry(".csv", Set.of("text/csv", "application/vnd.ms-excel")),
            Map.entry(".png", Set.of("image/png")),
            Map.entry(".jpg", Set.of("image/jpeg")),
            Map.entry(".jpeg", Set.of("image/jpeg")),
            Map.entry(".zip", Set.of("application/zip", "application/x-zip-compressed"))
    );

    public String validateAndGetFileName(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please select a non-empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size must not exceed 10 MB.");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("Invalid filename.");
        }

        // Remove any directory path supplied in the filename.
        String sanitizedFileName = originalFileName.replace('\\', '/');
        sanitizedFileName = sanitizedFileName.substring(
                sanitizedFileName.lastIndexOf('/') + 1
        );

        if (sanitizedFileName.isBlank()
                || sanitizedFileName.equals(".")
                || sanitizedFileName.equals("..")) {
            throw new IllegalArgumentException("Invalid filename.");
        }

        int dotIndex = sanitizedFileName.lastIndexOf('.');

        if (dotIndex <= 0 || dotIndex == sanitizedFileName.length() - 1) {
            throw new IllegalArgumentException("File must have a valid extension.");
        }

        String extension = sanitizedFileName.substring(dotIndex).toLowerCase();

        Set<String> allowedMimeTypes = ALLOWED_MIME_TYPES.get(extension);

        if (allowedMimeTypes == null) {
            throw new IllegalArgumentException(
                    "Unsupported file format: " + extension
            );
        }

        String contentType = file.getContentType();

        if (contentType != null
                && !contentType.isBlank()
                && !contentType.equalsIgnoreCase("application/octet-stream")
                && !allowedMimeTypes.contains(contentType.toLowerCase())) {

            throw new IllegalArgumentException(
                    "File content type does not match its extension."
            );
        }

        return sanitizedFileName;
    }
}
