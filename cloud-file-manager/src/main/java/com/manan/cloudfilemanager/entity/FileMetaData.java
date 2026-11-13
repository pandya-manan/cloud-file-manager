package com.manan.cloudfilemanager.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name="file_metadata",uniqueConstraints= {
		@UniqueConstraint(name="uk_bucket_object_key",columnNames= {"bucket_name","s3_object_key"})
})
public class FileMetaData {
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	private Long id;
	
	@Column(name="original_file_name",nullable=false,length=255)
	private String originalFileName;
	
	@Column(name="stored_file_name",nullable=false,length=255)
	private String storedFileName;
	
	@Column(name="s3_object_key",nullable=false,length=1024)
	private String s3ObjectKey;
	
	@Column(name="bucket_name",nullable=false,length=63)
	private String bucketName;
	
	@Column(name="content_type",length=150)
	private String contentType;
	
	@Column(name="file_size",nullable = false)
	private Long fileSize;
	
	@Column(name="uploaded_at",nullable=false,updatable=false)
	private LocalDateTime uploadedAt;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	public FileMetaData() {
		
	}
	
	@PrePersist
	protected void onCreate()
	{
		if(uploadedAt==null)
		{
			uploadedAt = LocalDateTime.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOriginalFileName() {
		return originalFileName;
	}

	public void setOriginalFileName(String originalFileName) {
		this.originalFileName = originalFileName;
	}

	public String getStoredFileName() {
		return storedFileName;
	}

	public void setStoredFileName(String storedFileName) {
		this.storedFileName = storedFileName;
	}

	public String getS3ObjectKey() {
		return s3ObjectKey;
	}

	public void setS3ObjectKey(String s3ObjectKey) {
		this.s3ObjectKey = s3ObjectKey;
	}

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public LocalDateTime getUploadedAt() {
		return uploadedAt;
	}

	public void setUploadedAt(LocalDateTime uploadedAt) {
		this.uploadedAt = uploadedAt;
	}

	public AppUser getUser() {
		return user;
	}

	public void setUser(AppUser user) {
		this.user = user;
	}
	
	
	
	
	
	

}
