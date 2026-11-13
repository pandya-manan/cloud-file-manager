
package com.manan.cloudfilemanager.service;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;

@Service
public class S3Service {

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;
	private final String bucketName;

	public S3Service(S3Client s3Client, S3Presigner s3Presigner, @Value("${aws.s3.bucket-name}") String bucketName) {

		this.s3Client = s3Client;
		this.s3Presigner = s3Presigner;
		this.bucketName = bucketName;
	}

	// 1. Upload file
	public String uploadFile(MultipartFile file) throws IOException {

		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File cannot be empty");
		}

		String originalFileName = file.getOriginalFilename();

		if (originalFileName == null || originalFileName.isBlank()) {
			throw new IllegalArgumentException("Invalid filename");
		}

		String sanitizedFileName = originalFileName.replace('\\', '/');

		sanitizedFileName = sanitizedFileName.substring(sanitizedFileName.lastIndexOf('/') + 1);

		if (sanitizedFileName.isBlank()) {
			throw new IllegalArgumentException("Invalid filename");
		}

		String extension = "";
		int dotIndex = sanitizedFileName.lastIndexOf('.');

		if (dotIndex > 0) {
			extension = sanitizedFileName.substring(dotIndex);
		}

		String storedFileName = UUID.randomUUID() + extension;
		String objectKey = "uploads/" + storedFileName;

		String contentType = file.getContentType();

		if (contentType == null || contentType.isBlank()) {
			contentType = "application/octet-stream";
		}

		PutObjectRequest request = PutObjectRequest.builder().bucket(bucketName).key(objectKey).contentType(contentType)
				.contentLength(file.getSize()).build();

		try (var inputStream = file.getInputStream()) {

			s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
		}

		return objectKey;
	}

	// 2. Delete file from S3
	// Delete file from S3 and return deletion details
	public DeleteObjectResponse deleteFile(String objectKey) {

		DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build();

		return s3Client.deleteObject(request);
	}

	// Restore deleted file
	// Restore file visibility by removing the delete marker
	public void restoreDeletedFile(String objectKey, String versionId) {

		DeleteObjectRequest request = DeleteObjectRequest.builder().bucket(bucketName).key(objectKey)
				.versionId(versionId).build();

		s3Client.deleteObject(request);
	}

	// 3. Download file from S3
	public ResponseBytes<GetObjectResponse> downloadFile(String objectKey) {

		GetObjectRequest request = GetObjectRequest.builder().bucket(bucketName).key(objectKey).build();

		return s3Client.getObjectAsBytes(request);
	}

	// 4. Generate presigned download URL

	// 4. Generate presigned download URL
	public String generatePresignedUrl(String objectKey, String originalFileName) {

		String contentDisposition = ContentDisposition.attachment().filename(originalFileName, StandardCharsets.UTF_8)
				.build().toString();

		GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(objectKey)
				.responseContentDisposition(contentDisposition).build();

		GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(Duration.ofMinutes(10)).getObjectRequest(getObjectRequest).build();

		PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

		return presignedRequest.url().toString();
	}

}
