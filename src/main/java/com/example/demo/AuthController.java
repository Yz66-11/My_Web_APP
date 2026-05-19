package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import org.springframework.security.core.context.SecurityContextHolder;
import java.security.Principal;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;

@Controller
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final GalleryService galleryService;
    private final ShopService shopService;
    private final DishRepository dishRepository;
    private final GalleryUnlockRepository galleryUnlockRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder,
                          EmailService emailService, GalleryService galleryService,
                          ShopService shopService, DishRepository dishRepository,
                          GalleryUnlockRepository galleryUnlockRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.galleryService = galleryService;
        this.shopService = shopService;
        this.dishRepository = dishRepository;
        this.galleryUnlockRepository = galleryUnlockRepository;
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
                Long userId = user.getId();
                model.addAttribute("username", user.getUsername());
                model.addAttribute("email", user.getEmail());
                model.addAttribute("name", user.getName());
                model.addAttribute("phone", user.getPhone());
                model.addAttribute("gender", user.getGender());
                model.addAttribute("age", user.getAge());
                model.addAttribute("bio", user.getBio());
                model.addAttribute("displayGender", user.getDisplayGender());
                model.addAttribute("avatarUrl", user.getAvatarUrl());
                model.addAttribute("checkinCount", galleryUnlockRepository.countByUserId(userId));
                model.addAttribute("galleryCount", galleryUnlockRepository.countByUserId(userId));
                model.addAttribute("favCount", userService.countUserFavorites(userId));
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

    @PostMapping("/delete-account")
    public String deleteAccount(Principal principal, HttpServletRequest request) {
        if (principal == null) {
            return "redirect:/login";
        }
        try {
            Long userId = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在")).getId();
            userService.deleteUser(userId);
            // 清除安全上下文并登出
            SecurityContextHolder.clearContext();
            request.getSession().invalidate();
            return "redirect:/login?logout";
        } catch (RuntimeException e) {
            return "redirect:/profile?error=" + java.net.URLEncoder.encode(
                    "注销失败：" + e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("file") MultipartFile file,
                                Principal principal, RedirectAttributes redirectAttributes) {
        try {
            if (file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "请选择图片");
                return "redirect:/profile";
            }
            // 校验文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                redirectAttributes.addFlashAttribute("error", "只支持图片文件");
                return "redirect:/profile";
            }
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            String imageUrl = saveAvatarFile(file, user.getId());
            user.setAvatarUrl(imageUrl);
            userService.saveUser(user);
            redirectAttributes.addFlashAttribute("success", "头像更新成功！");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "头像上传失败：" + e.getMessage());
        }
        return "redirect:/profile";
    }

    @GetMapping("/checkin")
    public String showCheckin(@RequestParam(value = "dishId", required = false) Long dishId,
                              @RequestParam(value = "shopId", required = false) Long shopId,
                              @RequestParam(value = "city", required = false) String city,
                              @RequestParam(value = "district", required = false) String district,
                              Model model, Principal principal) {
        if (dishId != null) {
            dishRepository.findById(dishId).ifPresent(dish -> {
                model.addAttribute("dish", dish);
                model.addAttribute("dishId", dishId);
            });
        }
        if (shopId != null) {
            shopService.findById(shopId).ifPresent(shop -> {
                model.addAttribute("shop", shop);
                model.addAttribute("shopId", shopId);
            });
        }
        model.addAttribute("city", city);
        model.addAttribute("district", district);
        return "checkin";
    }

    @PostMapping("/checkin")
    @ResponseBody
    public String processCheckin(@RequestBody CheckinRequest checkinRequest, Principal principal) {
        try {
            Long userId = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户未登录")).getId();
            Long dishId = checkinRequest.getDishId();
            if (dishId == null) {
                return "{\"success\": false, \"message\": \"缺少菜品ID\"}";
            }
            // 保存照片
            String imageUrl = null;
            String rawImage = checkinRequest.getImage();
            if (rawImage != null && !rawImage.isBlank()) {
                imageUrl = saveCheckinImage(rawImage, userId);
            }
            boolean newlyUnlocked = galleryService.unlockDish(userId, dishId, imageUrl, checkinRequest.getComment());
            String message = newlyUnlocked ? "打卡成功！图鉴已更新" : "该菜品已经解锁过了";
            return "{\"success\": true, \"message\": \"" + message + "\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    private String saveCheckinImage(String base64Data, Long userId) throws Exception {
        // 移除 data:image/jpeg;base64, 等前缀
        String data = base64Data;
        if (data.contains(",")) {
            data = data.substring(data.indexOf(",") + 1);
        }
        byte[] imageBytes = Base64.getDecoder().decode(data);

        String dirPath = uploadDir + "/checkin/";
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        String filename = userId + "_" + System.currentTimeMillis() + ".jpg";
        File file = new File(dirPath + filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageBytes);
        }

        return "/uploads/checkin/" + filename;
    }

    private String saveAvatarFile(MultipartFile file, Long userId) throws Exception {
        String dirPath = uploadDir + "/avatars/";
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        // 获取文件扩展名
        String originalName = file.getOriginalFilename();
        String ext = "jpg";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".") + 1);
        }
        String filename = userId + "_" + System.currentTimeMillis() + "." + ext;
        File dest = new File(dirPath + filename);
        file.transferTo(dest);

        return "/uploads/avatars/" + filename;
    }

    // ==================== Forgot Password ====================

    private static final int CODE_EXPIRY_MINUTES = 5;

    @GetMapping("/forgot-password")
    public String showForgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                       HttpSession session, Model model) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "该邮箱未注册！");
            return "forgot-password";
        }
        // 生成 6 位数字验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        session.setAttribute("resetCode", code);
        session.setAttribute("resetUsername", userOpt.get().getUsername());
        session.setAttribute("resetCodeTime", System.currentTimeMillis());

        // 发送邮件
        try {
            emailService.sendPasswordResetCode(email, code);
        } catch (Exception e) {
            model.addAttribute("error", "邮件发送失败，请稍后重试");
            return "forgot-password";
        }

        // 不暴露验证码，只提示已发送
        model.addAttribute("email", email);
        return "ott-sent";
    }

    @GetMapping("/verify-token")
    public String showVerifyToken(Model model) {
        return "verify-token";
    }

    @PostMapping("/verify-token")
    public String processVerifyToken(@RequestParam("code") String code,
                                     HttpSession session, Model model) {
        String sessionCode = (String) session.getAttribute("resetCode");
        Long codeTime = (Long) session.getAttribute("resetCodeTime");

        // 检查是否存在
        if (sessionCode == null || codeTime == null) {
            model.addAttribute("error", "请先获取验证码");
            return "verify-token";
        }
        // 检查是否过期（5分钟）
        if (System.currentTimeMillis() - codeTime > CODE_EXPIRY_MINUTES * 60 * 1000) {
            session.removeAttribute("resetCode");
            session.removeAttribute("resetUsername");
            session.removeAttribute("resetCodeTime");
            model.addAttribute("error", "验证码已过期，请重新获取");
            return "verify-token";
        }
        // 检查是否匹配
        if (!sessionCode.equals(code.trim())) {
            model.addAttribute("error", "验证码错误，请重新输入");
            return "verify-token";
        }

        // 验证通过
        session.removeAttribute("resetCode");
        session.removeAttribute("resetCodeTime");
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
    private Long dishId;
    private Long shopId;
    private String city;
    private String district;
    private String comment;
    private String image;

    public Long getDishId() { return dishId; }
    public void setDishId(Long dishId) { this.dishId = dishId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
}


