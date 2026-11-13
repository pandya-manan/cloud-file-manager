
package com.manan.cloudfilemanager.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.manan.cloudfilemanager.dto.SignupRequest;
import com.manan.cloudfilemanager.entity.AppUser;
import com.manan.cloudfilemanager.entity.Role;
import com.manan.cloudfilemanager.repository.AppUserRepository;

@Service
public class SignupService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {

        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void registerUser(SignupRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException(
                    "Passwords do not match."
            );
        }

        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        AppUser user = new AppUser();

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);

        // Never store plain-text passwords.
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Public signup always creates USER.
        user.setRole(Role.USER);
        user.setEnabled(true);

        appUserRepository.save(user);
    }
}
