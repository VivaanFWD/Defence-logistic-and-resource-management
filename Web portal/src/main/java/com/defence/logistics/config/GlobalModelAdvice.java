package com.defence.logistics.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.Map;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {
    @ModelAttribute("currentUser")
    public Map<String, Object> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            return Map.of("username", "Guest", "role", "GUEST");
        String role = auth.getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).findFirst().orElse("QUARTERMASTER");
        return Map.of("username", auth.getName(), "role", role);
    }
}
