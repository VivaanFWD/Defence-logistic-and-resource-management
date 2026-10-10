package com.defence.logistics.config;

import com.defence.logistics.auth.repository.AppUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> users.findByUsernameIgnoreCase(username)
                .map(account -> User.withUsername(account.getUsername())
                        .password(account.getPasswordHash()).disabled(!account.isEnabled())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole())))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean
    AuthenticationSuccessHandler roleRedirectHandler() {
        return (request, response, authentication) -> {
            String role = authentication.getAuthorities().stream().map(a -> a.getAuthority()).findFirst().orElse("");
            String target = switch (role) {
                case "ROLE_ADMIN" -> "/admin/dashboard";
                case "ROLE_LOGISTIC_OFFICER" -> "/logistics/dashboard";
                default -> "/requester/dashboard";
            };
            response.sendRedirect(request.getContextPath() + target);
        };
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AuthenticationSuccessHandler roleRedirectHandler) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/auth/login", "/register", "/css/**", "/script/**", "/images/**", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**", "/vehicle_mgmnt").hasRole("ADMIN")
                .requestMatchers("/logistics/**").hasAnyRole("ADMIN", "LOGISTIC_OFFICER")
                .requestMatchers("/requester/**").hasAnyRole("ADMIN", "QUARTERMASTER")
                .requestMatchers("/profile").authenticated()
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                    .successHandler(roleRedirectHandler).failureUrl("/login?error=true").permitAll())
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout=true").invalidateHttpSession(true).deleteCookies("JSESSIONID").permitAll())
            .sessionManagement(session -> session.sessionFixation(fixation -> fixation.migrateSession()));
        return http.build();
    }
}
