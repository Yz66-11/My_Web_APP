package com.example.demo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/")
    public String home(Model model, Principal principal) {
        if (principal != null) {
            model.addAttribute("username", principal.getName());
            Optional<User> userOpt = userService.findByUsername(principal.getName());
            userOpt.ifPresent(user -> model.addAttribute("isAdmin", user.isAdmin()));
        }
        return "index";
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "registered", required = false) String registered,
                                @RequestParam(value = "reset", required = false) String reset,
                                Model model) {
        if (error != null) {
            model.addAttribute("error", java.net.URLDecoder.decode(error, java.nio.charset.StandardCharsets.UTF_8));
        }
        if (logout != null) model.addAttribute("success", "您已成功退出登录！");
        if (registered != null) model.addAttribute("success", "注册成功！请登录您的账户。");
        if (reset != null) model.addAttribute("success", "密码重置成功！请使用新密码登录。");
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage(@RequestParam(value = "success", required = false) String success,
                                   Model model) {
        model.addAttribute("user", new User());
        if (success != null) model.addAttribute("success", "注册成功！请登录您的账户。");
        return "register";
    }

    @PostMapping("/register")
    public String processRegistration(@ModelAttribute("user") @Valid User user,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      BindingResult result, Model model) {
        if (!user.getPassword().equals(confirmPassword)) {
            model.addAttribute("error", "两次输入的密码不一致！");
            return "register";
        }
        if (result.hasErrors()) {
            model.addAttribute("error", "表单填写有误，请检查！");
            return "register";
        }
        if (userService.existsByUsername(user.getUsername())) {
            model.addAttribute("error", "用户名已存在！");
            return "register";
        }
        if (userService.existsByEmail(user.getEmail())) {
            model.addAttribute("error", "邮箱已被注册！");
            return "register";
        }
        userService.registerUser(user);
        return "redirect:/login?registered";
    }

    @GetMapping("/profile")
    public String showProfile(@RequestParam(value = "success", required = false) String success,
                              @RequestParam(value = "error", required = false) String error,
                              Model model, Principal principal) {
        if (principal != null) {
            userService.findByUsername(principal.getName()).ifPresent(user -> {
                model.addAttribute("username", user.getUsername());
                model.addAttribute("email", user.getEmail());
                model.addAttribute("name", user.getName());
                model.addAttribute("phone", user.getPhone());
                model.addAttribute("gender", user.getGender());
                model.addAttribute("age", user.getAge());
                model.addAttribute("bio", user.getBio());
                model.addAttribute("displayGender", user.getDisplayGender());
            });
        }
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        return "profile";
    }

    @PostMapping("/update-profile")
    public String updateProfile(@RequestParam(value = "name", required = false) String name,
                                @RequestParam(value = "phone", required = false) String phone,
                                @RequestParam(value = "gender", required = false) Integer gender,
                                @RequestParam(value = "age", required = false) Integer age,
                                @RequestParam(value = "bio", required = false) String bio,
                                Principal principal) {
        try {
            userService.updateProfile(principal.getName(), name, phone, gender, age, bio);
            return "redirect:/profile?success=" + java.net.URLEncoder.encode(
                    "个人信息修改成功！", java.nio.charset.StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return "redirect:/profile?error=" + java.net.URLEncoder.encode(
                    e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 Principal principal) {
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/profile?error=" + java.net.URLEncoder.encode(
                    "两次输入的新密码不一致！", java.nio.charset.StandardCharsets.UTF_8);
        }
        if (newPassword.length() < 6) {
            return "redirect:/profile?error=" + java.net.URLEncoder.encode(
                    "新密码长度不能少于6位！", java.nio.charset.StandardCharsets.UTF_8);
        }
        try {
            userService.changePassword(principal.getName(), oldPassword, newPassword);
            return "redirect:/profile?success=" + java.net.URLEncoder.encode(
                    "密码修改成功！", java.nio.charset.StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return "redirect:/profile?error=" + java.net.URLEncoder.encode(
                    e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/checkin")
    public String showCheckin(Model model) { return "checkin"; }

    @PostMapping("/checkin")
    @ResponseBody
    public String processCheckin(@RequestBody CheckinRequest checkinRequest, Principal principal) {
        return "{\"success\": true, \"message\": \"打卡成功！\"}";
    }

    // ==================== Forgot Password ====================

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("username") String username,
                                       HttpSession session, Model model) {
        if (!userService.existsByUsername(username)) {
            model.addAttribute("error", "用户名不存在！");
            return "forgot-password";
        }
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        session.setAttribute("resetToken", token);
        session.setAttribute("resetUsername", username);
        model.addAttribute("ottToken", token);
        return "ott-sent";
    }

    @GetMapping("/ott/sent")
    public String ottSent(HttpSession session, Model model) {
        String token = (String) session.getAttribute("resetToken");
        if (token != null) {
            model.addAttribute("ottToken", token);
        }
        return "ott-sent";
    }

    @GetMapping("/verify-token")
    public String showVerifyToken(@RequestParam(value = "token", required = false) String token, Model model) {
        model.addAttribute("token", token);
        return "verify-token";
    }

    @PostMapping("/verify-token")
    public String processVerifyToken(@RequestParam("token") String token,
                                     HttpSession session, Model model) {
        String sessionToken = (String) session.getAttribute("resetToken");
        if (sessionToken == null || !sessionToken.equals(token.trim().toUpperCase())) {
            model.addAttribute("error", "验证码无效或已过期，请重新获取");
            return "verify-token";
        }
        session.removeAttribute("resetToken");
        session.setAttribute("tokenVerified", true);
        return "redirect:/reset-password";
    }

    @GetMapping("/reset-password")
    public String showResetPassword(HttpSession session, Model model) {
        Boolean verified = (Boolean) session.getAttribute("tokenVerified");
        String username = (String) session.getAttribute("resetUsername");
        if (!Boolean.TRUE.equals(verified) || username == null) {
            return "redirect:/forgot-password";
        }
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam("newPassword") String newPassword,
                                @RequestParam("confirmPassword") String confirmPassword,
                                HttpSession session) {
        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/reset-password?error=" + java.net.URLEncoder.encode(
                    "两次输入的密码不一致！", java.nio.charset.StandardCharsets.UTF_8);
        }
        if (newPassword.length() < 6) {
            return "redirect:/reset-password?error=" + java.net.URLEncoder.encode(
                    "密码长度不能少于6位！", java.nio.charset.StandardCharsets.UTF_8);
        }
        String username = (String) session.getAttribute("resetUsername");
        try {
            userService.resetPassword(username, newPassword);
            session.removeAttribute("resetUsername");
            session.removeAttribute("tokenVerified");
            return "redirect:/login?reset";
        } catch (RuntimeException e) {
            return "redirect:/reset-password?error=" + java.net.URLEncoder.encode(
                    e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}

class CheckinRequest {
    private String foodType;
    private String comment;
    private String image;
    private Location location;
    private String timestamp;

    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}

class Location {
    private double latitude;
    private double longitude;

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
