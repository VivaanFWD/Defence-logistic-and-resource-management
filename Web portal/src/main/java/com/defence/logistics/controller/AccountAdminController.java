package com.defence.logistics.controller;

import com.defence.logistics.auth.service.AccountAdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountAdminController {
    private final AccountAdminService accounts;
    public AccountAdminController(AccountAdminService accounts) { this.accounts = accounts; }

    @GetMapping("/admin/accounts")
    public String accounts(Model model) {
        model.addAttribute("accounts", accounts.findAll());
        return "admin/accounts";
    }

    @PostMapping("/admin/accounts/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam String role, RedirectAttributes flash) {
        try { accounts.updateRole(id, role); flash.addFlashAttribute("success", "Account role updated."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/admin/accounts/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam boolean enabled, RedirectAttributes flash) {
        try { accounts.updateEnabled(id, enabled); flash.addFlashAttribute("success", "Account status updated."); }
        catch (IllegalArgumentException ex) { flash.addFlashAttribute("error", ex.getMessage()); }
        return "redirect:/admin/accounts";
    }
}
