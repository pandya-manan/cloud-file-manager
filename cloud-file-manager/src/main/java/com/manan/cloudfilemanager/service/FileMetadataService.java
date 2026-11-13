
package com.manan.cloudfilemanager.service;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.manan.cloudfilemanager.entity.AppUser;
import com.manan.cloudfilemanager.entity.FileMetaData;
import com.manan.cloudfilemanager.repository.FileMetadataRepository;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Service
public class FileMetadataService {

    private final S3Service s3Service;
    private final FileMetadataRepository repository;
    private final String bucketName;
    private final FileValidationService fileValidationService;

    public FileMetadataService(
            S3Service s3Service,
            FileMetadataRepository repository,
            @Value("${aws.s3.bucket-name}") String bucketName,
            FileValidationService fileValidationService) {

        this.s3Service = s3Service;
        this.repository = repository;
        this.bucketName = bucketName;
        this.fileValidationService = fileValidationService;
    }

    // 1. Upload file and associate it with the owner

    public FileMetaData uploadFile(MultipartFile file, AppUser user)
            throws IOException {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("Valid user is required.");
        }

        String sanitizedFileName =
                fileValidationService.validateAndGetFileName(file);

        String objectKey = s3Service.uploadFile(file);

        try {

            FileMetaData metadata = new FileMetaData();

            metadata.setOriginalFileName(sanitizedFileName);
            metadata.setStoredFileName(
                    objectKey.substring(objectKey.lastIndexOf('/') + 1)
            );
            metadata.setS3ObjectKey(objectKey);
            metadata.setBucketName(bucketName);
            metadata.setContentType(file.getContentType());
            metadata.setFileSize(file.getSize());

            // Associate uploaded file with its owner
            metadata.setUser(user);

            return repository.save(metadata);

        } catch (RuntimeException exception) {

            // Compensating cleanup if metadata persistence fails
            try {
                s3Service.deleteFile(objectKey);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }

    // 2. Get all files belonging to a user

    public Page<FileMetaData> getAllFiles(
            Long userId, int page, int size) {

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(page, size);

        return repository.findByUser_Id(userId, pageable);
    }

    // 3. Get a file only if it belongs to the user

    public FileMetaData getFileById(Long id, Long userId) {

        return repository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "File not found."
                ));
    }

    // 4. Search files belonging to a user

    public Page<FileMetaData> searchFiles(
            String name, Long userId, int page, int size) {

        validatePagination(page, size);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Search name cannot be empty"
            );
        }

        Pageable pageable = PageRequest.of(page, size);

        return repository
                .findByUser_IdAndOriginalFileNameContainingIgnoreCase(
                        userId, name.trim(), pageable
                );
    }

    // 5. Download only the user's own file

    public ResponseBytes<GetObjectResponse> downloadFile(
            Long id, Long userId) {

        FileMetaData metadata = getFileById(id, userId);

        return s3Service.downloadFile(metadata.getS3ObjectKey());
    }

    // 6. Generate presigned URL only for the user's own file

    public String generatePresignedUrl(Long id, Long userId) {

        FileMetaData metadata = getFileById(id, userId);

        return s3Service.generatePresignedUrl(
                metadata.getS3ObjectKey(),
                metadata.getOriginalFileName()
        );
    }

    // 7. Delete only the user's own file

    public void deleteFile(Long id, Long userId) {

        FileMetaData metadata = getFileById(id, userId);

        // Step 1: Delete object from S3
        DeleteObjectResponse deleteResponse =
                s3Service.deleteFile(metadata.getS3ObjectKey());

        String versionId = deleteResponse.versionId();

        boolean deleteMarkerCreated =
                Boolean.TRUE.equals(deleteResponse.deleteMarker());

        try {

            // Step 2: Delete metadata from MySQL
            repository.delete(metadata);

        } catch (RuntimeException exception) {

            // Step 3: Restore file visibility if MySQL deletion fails
            if (deleteMarkerCreated
                    && versionId != null
                    && !versionId.isBlank()) {

                try {
                    s3Service.restoreDeletedFile(
                            metadata.getS3ObjectKey(), versionId
                    );

                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }

            throw exception;
        }
    }

    private void validatePagination(int page, int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }
    }
}
