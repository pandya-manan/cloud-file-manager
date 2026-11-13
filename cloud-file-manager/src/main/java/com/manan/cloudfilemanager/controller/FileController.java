
package com.manan.cloudfilemanager.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.manan.cloudfilemanager.dto.FileMetadataResponse;
import com.manan.cloudfilemanager.dto.PresignedUrlResponse;
import com.manan.cloudfilemanager.entity.AppUser;
import com.manan.cloudfilemanager.entity.FileMetaData;
import com.manan.cloudfilemanager.repository.AppUserRepository;
import com.manan.cloudfilemanager.service.FileMetadataService;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileMetadataService fileMetadataService;
    private final AppUserRepository appUserRepository;

    public FileController(
            FileMetadataService fileMetadataService,
            AppUserRepository appUserRepository) {

        this.fileMetadataService = fileMetadataService;
        this.appUserRepository = appUserRepository;
    }

    // Get currently authenticated user
    private AppUser getCurrentUser(Authentication authentication) {

        return appUserRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user not found."
                ));
    }

    // 1. Upload file
    @PostMapping("/upload")
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        AppUser currentUser = getCurrentUser(authentication);

        FileMetaData metadata =
                fileMetadataService.uploadFile(file, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FileMetadataResponse.fromEntity(metadata));
    }

    // 2. Get all files with pagination
    @GetMapping
    public ResponseEntity<Page<FileMetadataResponse>> getAllFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long userId = getCurrentUser(authentication).getId();

        Page<FileMetadataResponse> response =
                fileMetadataService.getAllFiles(userId, page, size)
                        .map(FileMetadataResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    // 3. Get file by ID
    @GetMapping("/{id}")
    public ResponseEntity<FileMetadataResponse> getFileById(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getCurrentUser(authentication).getId();

        FileMetadataResponse response =
                FileMetadataResponse.fromEntity(
                        fileMetadataService.getFileById(id, userId)
                );

        return ResponseEntity.ok(response);
    }

    // 4. Search files with pagination
    @GetMapping("/search")
    public ResponseEntity<Page<FileMetadataResponse>> searchFiles(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long userId = getCurrentUser(authentication).getId();

        Page<FileMetadataResponse> response =
                fileMetadataService.searchFiles(
                        name, userId, page, size
                ).map(FileMetadataResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    // 5. Direct download
    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getCurrentUser(authentication).getId();

        FileMetaData metadata =
                fileMetadataService.getFileById(id, userId);

        ResponseBytes<GetObjectResponse> file =
                fileMetadataService.downloadFile(id, userId);

        byte[] fileBytes = file.asByteArray();

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;

        if (metadata.getContentType() != null) {
            try {
                contentType = MediaType.parseMediaType(
                        metadata.getContentType()
                );
            } catch (IllegalArgumentException ignored) {
                // Retain application/octet-stream.
            }
        }

        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(
                                metadata.getOriginalFileName(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .contentLength(fileBytes.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(new ByteArrayResource(fileBytes));
    }

    // 6. Generate presigned download URL
    @GetMapping("/{id}/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getCurrentUser(authentication).getId();

        String url =
                fileMetadataService.generatePresignedUrl(id, userId);

        return ResponseEntity.ok(
                new PresignedUrlResponse(url, 600)
        );
    }

    // 7. Delete file
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getCurrentUser(authentication).getId();

        fileMetadataService.deleteFile(id, userId);

        return ResponseEntity.ok(
                Map.of("message", "File deleted successfully")
        );
    }
}
