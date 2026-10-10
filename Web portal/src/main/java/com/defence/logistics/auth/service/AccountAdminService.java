package com.defence.logistics.auth.service;

import com.defence.logistics.auth.entity.AppUser;
import com.defence.logistics.auth.repository.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AccountAdminService {
    private static final List<String> ALLOWED_ROLES = List.of("ADMIN", "LOGISTIC_OFFICER", "QUARTERMASTER");
    private final AppUserRepository users;
    public AccountAdminService(AppUserRepository users) { this.users = users; }
    public List<AppUser> findAll() { return users.findAll(org.springframework.data.domain.Sort.by("createdAt").descending()); }
    @Transactional public void updateRole(Long id, String role) {
        if (!ALLOWED_ROLES.contains(role)) throw new IllegalArgumentException("Unsupported role.");
        AppUser account = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found."));
        account.setRole(role);
    }
    @Transactional public void updateEnabled(Long id, boolean enabled) {
        AppUser account = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Account not found."));
        account.setEnabled(enabled);
    }
}
