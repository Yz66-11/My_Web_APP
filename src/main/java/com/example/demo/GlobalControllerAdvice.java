package com.example.demo;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;

    public GlobalControllerAdvice(UserService userService) {
        this.userService = userService;
    }

    @ModelAttribute
    public void addUserAttributes(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
            userService.findByUsername(principal.getName()).ifPresent(user -> {
                model.addAttribute("avatarUrl", user.getAvatarUrl());
                model.addAttribute("userId", user.getId());
                model.addAttribute("petPoints", user.getPetPoints());
            });
        }
    }
}
