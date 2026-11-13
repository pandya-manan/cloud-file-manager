package com.manan.cloudfilemanager.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.manan.cloudfilemanager.dto.admin.AdminDashboardStats;
import com.manan.cloudfilemanager.dto.admin.AdminFileResponse;
import com.manan.cloudfilemanager.dto.admin.AdminUserResponse;
import com.manan.cloudfilemanager.entity.Role;
import com.manan.cloudfilemanager.repository.AppUserRepository;
import com.manan.cloudfilemanager.repository.FileMetadataRepository;

@Service
public class AdminService {

    private final AppUserRepository appUserRepository;
    private final FileMetadataRepository fileMetadataRepository;

    public AdminService(
            AppUserRepository appUserRepository,
            FileMetadataRepository fileMetadataRepository) {
        this.appUserRepository = appUserRepository;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardStats getDashboardStats() {

        long totalUsers = appUserRepository.count();
        long totalAdmins = appUserRepository.countByRole(Role.ADMIN);
        long totalFiles = fileMetadataRepository.count();

        return new AdminDashboardStats(
                totalUsers,
                totalAdmins,
                totalFiles
        );
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> getAllUsers() {

        return appUserRepository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent()
         .stream()
         .map(user -> new AdminUserResponse(
                 user.getId(),
                 user.getFullName(),
                 user.getEmail(),
                 user.getRole(),
                 user.isEnabled(),
                 user.getCreatedAt()
         ))
         .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminFileResponse> getAllFiles() {

        return fileMetadataRepository.findAll(
                PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "uploadedAt"))
        ).getContent()
         .stream()
         .map(file -> new AdminFileResponse(
                 file.getId(),
                 file.getOriginalFileName(),
                 file.getContentType(),
                 file.getFileSize(),
                 file.getUploadedAt(),
                 file.getUser().getFullName(),
                 file.getUser().getEmail()
         ))
         .toList();
    }
}