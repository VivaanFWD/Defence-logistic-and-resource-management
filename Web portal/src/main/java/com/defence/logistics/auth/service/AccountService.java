package com.defence.logistics.auth.service;

import com.defence.logistics.auth.entity.AppUser;
import com.defence.logistics.auth.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AccountService {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    public AccountService(AppUserRepository users, PasswordEncoder encoder) { this.users = users; this.encoder = encoder; }

    @Transactional
    public void registerRequester(String fullName, String username, String email, String password) {
        String cleanName = fullName == null ? "" : fullName.trim();
        String cleanUser = username == null ? "" : username.trim();
        String cleanEmail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (cleanName.isBlank() || cleanUser.length() < 3 || !cleanEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            throw new IllegalArgumentException("Enter a name, username (at least 3 characters), and valid email.");
        if (password == null || password.length() < 8) throw new IllegalArgumentException("Password must contain at least 8 characters.");
        if (users.existsByUsernameIgnoreCase(cleanUser)) throw new IllegalArgumentException("That username is already in use.");
        if (users.existsByEmailIgnoreCase(cleanEmail)) throw new IllegalArgumentException("That email is already registered.");
        // Public registration can never grant administrative or staff privileges.
        users.save(new AppUser(cleanName, cleanUser, cleanEmail, encoder.encode(password), "QUARTERMASTER"));
    }
}
