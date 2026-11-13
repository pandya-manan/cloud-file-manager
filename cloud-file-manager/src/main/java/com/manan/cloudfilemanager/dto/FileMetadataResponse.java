
package com.manan.cloudfilemanager.dto;

import java.time.LocalDateTime;

import com.manan.cloudfilemanager.entity.FileMetaData;

public record FileMetadataResponse(
        Long id,
        String originalFileName,
        String contentType,
        Long fileSize,
        LocalDateTime uploadedAt
) {

    public static FileMetadataResponse fromEntity(FileMetaData metadata) {

        return new FileMetadataResponse(
                metadata.getId(),
                metadata.getOriginalFileName(),
                metadata.getContentType(),
                metadata.getFileSize(),
                metadata.getUploadedAt()
        );
    }
}
