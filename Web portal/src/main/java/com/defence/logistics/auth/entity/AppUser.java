package com.defence.logistics.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_app_users_email", columnNames = "email")
})
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80) private String fullName;
    @Column(nullable = false, length = 40) private String username;
    @Column(nullable = false, length = 160) private String email;
    @Column(nullable = false, length = 100) private String passwordHash;
    @Column(nullable = false, length = 30) private String role = "QUARTERMASTER";
    @Column(nullable = false) private boolean enabled = true;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();

    protected AppUser() {}
    public AppUser(String fullName, String username, String email, String passwordHash, String role) {
        this.fullName = fullName; this.username = username; this.email = email;
        this.passwordHash = passwordHash; this.role = role; this.enabled = true; this.createdAt = Instant.now();
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setRole(String role) { this.role = role; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
