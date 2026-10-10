package com.defence.logistics.config;

import com.defence.logistics.auth.entity.AppUser;
import com.defence.logistics.auth.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdmin {
    @Bean
    ApplicationRunner createInitialAdmin(AppUserRepository users, PasswordEncoder encoder,
            @Value("${DEFENCE_ADMIN_USERNAME:}") String username,
            @Value("${DEFENCE_ADMIN_PASSWORD:}") String password,
            @Value("${DEFENCE_ADMIN_EMAIL:}") String email,
            @Value("${DEFENCE_ADMIN_FULL_NAME:System Administrator}") String fullName) {
        return args -> {
            // Set credentials through environment variables; never ship a default admin password.
            if (username.isBlank() || password.isBlank() || email.isBlank()) return;
            if (!users.existsByUsernameIgnoreCase(username.trim())) {
                users.save(new AppUser(fullName, username.trim(), email.trim().toLowerCase(), encoder.encode(password), "ADMIN"));
            }
        };
    }
}
