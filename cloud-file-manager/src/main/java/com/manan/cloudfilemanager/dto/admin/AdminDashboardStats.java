package com.manan.cloudfilemanager.dto.admin;

public record AdminDashboardStats(
        long totalUsers,
        long totalAdmins,
        long totalFiles
) {
}