package com.manan.cloudfilemanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.manan.cloudfilemanager.entity.AppUser;
import com.manan.cloudfilemanager.entity.Role;
import com.manan.cloudfilemanager.repository.AppUserRepository;

@Service
public class AdminInitializationService implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.full-name}")
    private String adminFullName;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminInitializationService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        long adminCount = appUserRepository.countByRole(Role.ADMIN);

        if (adminCount == 1) {
            System.out.println("Admin account already exists. Skipping initialization.");
            return;
        }

        if (adminCount > 1) {
            throw new IllegalStateException(
                    "Multiple admin accounts found. Expected exactly one admin account.");
        }

        if (adminEmail == null || adminEmail.isBlank()
                || adminPassword == null || adminPassword.length() < 12) {
            throw new IllegalStateException(
                    "Valid ADMIN_EMAIL and ADMIN_PASSWORD (minimum 12 characters) must be configured.");
        }

        AppUser admin = new AppUser();
        admin.setFullName(adminFullName);
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);

        appUserRepository.save(admin);

        System.out.println("Initial admin account created successfully.");
    }
}