
package com.manan.cloudfilemanager.controller;

import java.io.IOException;

import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.manan.cloudfilemanager.entity.AppUser;
import com.manan.cloudfilemanager.entity.FileMetaData;
import com.manan.cloudfilemanager.repository.AppUserRepository;
import com.manan.cloudfilemanager.service.FileMetadataService;

@Controller
public class PageController {

    private final FileMetadataService fileMetadataService;
    private final AppUserRepository appUserRepository;

    public PageController(
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

    // Display dashboard
    @GetMapping("/")
    public String dashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            Model model,
            Authentication authentication) {

        AppUser currentUser = getCurrentUser(authentication);

        Page<FileMetaData> files;

        if (name != null && !name.isBlank()) {

            files = fileMetadataService.searchFiles(
                    name, currentUser.getId(), page, size
            );

            model.addAttribute("searchName", name);

        } else {

            files = fileMetadataService.getAllFiles(
                    currentUser.getId(), page, size
            );
        }

        model.addAttribute("files", files);
        model.addAttribute("currentPage", page);
        model.addAttribute("currentUser", currentUser);

        return "dashboard";
    }

    // Upload file from dashboard
    @PostMapping("/files/upload")
    public String uploadFile(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes,
            Authentication authentication) throws IOException {

        AppUser currentUser = getCurrentUser(authentication);

        fileMetadataService.uploadFile(file, currentUser);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "File uploaded successfully!"
        );

        return "redirect:/";
    }

    // Delete file from dashboard
    @PostMapping("/files/{id}/delete")
    public String deleteFile(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Authentication authentication) {

        AppUser currentUser = getCurrentUser(authentication);

        fileMetadataService.deleteFile(id, currentUser.getId());

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "File deleted successfully!"
        );

        return "redirect:/";
    }
}
