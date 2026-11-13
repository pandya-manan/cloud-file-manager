package com.manan.cloudfilemanager.dto.admin;

import java.time.LocalDateTime;

public record AdminFileResponse(
        Long id,
        String originalFileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt,
        String ownerName,
        String ownerEmail
) {
}