package com.manan.cloudfilemanager.dto.admin;

import java.time.LocalDateTime;

import com.manan.cloudfilemanager.entity.Role;

public record AdminUserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean enabled,
        LocalDateTime createdAt
) {
}