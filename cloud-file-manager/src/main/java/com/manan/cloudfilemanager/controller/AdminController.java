package com.manan.cloudfilemanager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.manan.cloudfilemanager.service.AdminService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        model.addAttribute("stats", adminService.getDashboardStats());
        model.addAttribute("users", adminService.getAllUsers());
        model.addAttribute("files", adminService.getAllFiles());

        return "admin/admin-dashboard";
    }
}