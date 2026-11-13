
package com.manan.cloudfilemanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.manan.cloudfilemanager.dto.SignupRequest;
import com.manan.cloudfilemanager.service.SignupService;

import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final SignupService signupService;

    public AuthController(SignupService signupService) {
        this.signupService = signupService;
    }

    @GetMapping("/signup")
    public String signupPage(Model model) {

        if (!model.containsAttribute("signupRequest")) {
            model.addAttribute("signupRequest", new SignupRequest());
        }

        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute("signupRequest") SignupRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            return "signup";
        }

        try {
            signupService.registerUser(request);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Account created successfully. Please log in."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException exception) {

            bindingResult.reject(
                    "signup.error",
                    exception.getMessage()
            );

            return "signup";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}
