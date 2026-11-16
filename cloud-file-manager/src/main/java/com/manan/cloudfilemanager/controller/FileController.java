
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@RestController
@RequestMapping("/api/files")
@Tag(
    name = "File Management",
    description = "APIs for uploading, retrieving, searching, downloading and deleting user files"
)
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
    @Operation(
        summary = "Upload a file",
        description = "Uploads a file to Amazon S3 and stores its metadata in MySQL. The uploaded file belongs to the currently authenticated user. Maximum file size is 10 MB."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "File uploaded successfully",
            content = @Content(schema = @Schema(implementation = FileMetadataResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or validation failure"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "413", description = "File exceeds the maximum allowed size")
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileMetadataResponse> uploadFile(
            @Parameter(
                description = "File to upload (maximum 10 MB)",
                required = true
            )
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {

        AppUser currentUser = getCurrentUser(authentication);

        FileMetaData metadata =
                fileMetadataService.uploadFile(file, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FileMetadataResponse.fromEntity(metadata));
    }

    // 2. Get all files with pagination
    @Operation(
        summary = "Get all files",
        description = "Retrieves the authenticated user's files using pagination. Users can only access their own files."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ResponseEntity<Page<FileMetadataResponse>> getAllFiles(
            @Parameter(description = "Page number (zero-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of records per page", example = "10")
            @RequestParam(defaultValue = "10") int size,

            Authentication authentication) {

        Long userId = getCurrentUser(authentication).getId();

        Page<FileMetadataResponse> response =
                fileMetadataService.getAllFiles(userId, page, size)
                        .map(FileMetadataResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    // 3. Get file by ID
    @Operation(
        summary = "Get file metadata by ID",
        description = "Retrieves metadata for a specific file owned by the authenticated user."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File metadata retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "File not found or does not belong to the user")
    })
    @GetMapping("/{id}")
    public ResponseEntity<FileMetadataResponse> getFileById(
            @Parameter(description = "Unique file ID", example = "1", required = true)
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
    @Operation(
        summary = "Search files by name",
        description = "Searches the authenticated user's files by original filename, with pagination."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/search")
    public ResponseEntity<Page<FileMetadataResponse>> searchFiles(
            @Parameter(description = "Filename or partial filename to search for", example = "report", required = true)
            @RequestParam String name,

            @Parameter(description = "Page number (zero-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of records per page", example = "10")
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
    @Operation(
        summary = "Download a file",
        description = "Downloads the actual file from Amazon S3. Only the owner of the file can download it."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "File downloaded successfully",
            content = @Content(
                mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                schema = @Schema(type = "string", format = "binary")
            )
        ),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "File not found or does not belong to the user")
    })
    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadFile(
            @Parameter(description = "Unique file ID", example = "1", required = true)
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
    @Operation(
        summary = "Generate a presigned URL",
        description = "Generates a temporary Amazon S3 presigned URL for downloading a file. The URL is valid for 10 minutes."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Presigned URL generated successfully",
            content = @Content(schema = @Schema(implementation = PresignedUrlResponse.class))),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "File not found or does not belong to the user")
    })
    @GetMapping("/{id}/presigned-url")
    public ResponseEntity<PresignedUrlResponse> generatePresignedUrl(
            @Parameter(description = "Unique file ID", example = "1", required = true)
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
    @Operation(
        summary = "Delete a file",
        description = "Deletes the file from Amazon S3 and removes its metadata from MySQL. Only the file owner can delete it."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Authentication required"),
        @ApiResponse(responseCode = "404", description = "File not found or does not belong to the user")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFile(
            @Parameter(description = "Unique file ID", example = "1", required = true)
            @PathVariable Long id,

            Authentication authentication) {

        Long userId = getCurrentUser(authentication).getId();

        fileMetadataService.deleteFile(id, userId);

        return ResponseEntity.ok(
                Map.of("message", "File deleted successfully")
        );
    }
}
