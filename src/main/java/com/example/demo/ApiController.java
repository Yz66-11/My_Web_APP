package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * REST API Controller — Android 客户端专用
 * 所有接口以 /api/** 为前缀，返回 JSON
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserService userService;
    private final ShopService shopService;
    private final GalleryService galleryService;
    private final PostService postService;
    private final DishRepository dishRepository;
    private final GalleryUnlockRepository galleryUnlockRepository;
    private final ShopVisitRepository shopVisitRepository;
    private final AuthenticationManager authenticationManager;
    private final OrderService orderService;
    private final EmailService emailService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // 验证码过期时间（5分钟），与 Web 端一致
    private static final int CODE_EXPIRY_MINUTES = 5;

    public ApiController(UserService userService,
                         ShopService shopService,
                         GalleryService galleryService,
                         PostService postService,
                         DishRepository dishRepository,
                         GalleryUnlockRepository galleryUnlockRepository,
                         ShopVisitRepository shopVisitRepository,
                         AuthenticationManager authenticationManager,
                         OrderService orderService,
                         EmailService emailService) {
        this.userService = userService;
        this.shopService = shopService;
        this.galleryService = galleryService;
        this.postService = postService;
        this.dishRepository = dishRepository;
        this.galleryUnlockRepository = galleryUnlockRepository;
        this.shopVisitRepository = shopVisitRepository;
        this.authenticationManager = authenticationManager;
        this.orderService = orderService;
        this.emailService = emailService;
    }

    // ============================================================
    // AUTH
    // ============================================================

    /** POST /api/auth/login — 用户名密码登录，返回用户信息 + JWT Token 对 */
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String username = body.get("username");
        String password = body.get("password");
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(auth);
            // 兼容 Web 端 Session 认证
            HttpSession session = request.getSession(true);
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            // 生成双 Token
            String accessToken = JwtUtil.generateAccessToken(username);
            String refreshToken = JwtUtil.generateRefreshToken(username);

            Map<String, Object> result = buildUserMap(user);
            result.put("accessToken", accessToken);
            result.put("refreshToken", refreshToken);
            return ResponseEntity.ok(result);
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }
    }

    /** POST /api/auth/refresh — 使用 Refresh Token 换取新的 Token 对（无感刷新） */
    @PostMapping("/auth/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少 refreshToken"));
        }
        if (!JwtUtil.isTokenValid(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "refreshToken 无效或已过期"));
        }
        if (JwtUtil.isTokenExpired(refreshToken)) {
            return ResponseEntity.status(401).body(Map.of("error", "refreshToken 已过期，请重新登录"));
        }
        String username = JwtUtil.extractUsername(refreshToken);
        // 验证用户仍有效
        if (userService.findByUsername(username).isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "用户不存在"));
        }
        // 生成新的 Token 对
        String newAccessToken = JwtUtil.generateAccessToken(username);
        String newRefreshToken = JwtUtil.generateRefreshToken(username);
        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken
        ));
    }

    /** POST /api/auth/register — 注册新用户 */
    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> body, HttpSession session) {
        String username = body.get("username");
        String email = body.get("email");
        String password = body.get("password");
        String code = body.get("code");

        if (username == null || username.length() < 3 || username.length() > 20) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名长度须为3-20位"));
        }
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码不能少于6位"));
        }
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入邮箱"));
        }
        // 验证验证码
        String sessionCode = (String) session.getAttribute("apiRegisterCode");
        String sessionEmail = (String) session.getAttribute("apiRegisterEmail");
        Long codeTime = (Long) session.getAttribute("apiRegisterCodeTime");
        if (sessionCode == null || codeTime == null || sessionEmail == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "请先获取验证码"));
        }
        if (!sessionEmail.equals(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "邮箱不匹配，请重新获取验证码"));
        }
        if (System.currentTimeMillis() - codeTime > CODE_EXPIRY_MINUTES * 60 * 1000L) {
            session.removeAttribute("apiRegisterCode");
            session.removeAttribute("apiRegisterEmail");
            session.removeAttribute("apiRegisterCodeTime");
            return ResponseEntity.badRequest().body(Map.of("error", "验证码已过期，请重新获取"));
        }
        if (code == null || !sessionCode.equals(code.trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "验证码错误"));
        }
        if (userService.existsByUsername(username)) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名已存在"));
        }
        if (userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "邮箱已被注册"));
        }

        // 验证通过，清除验证码会话
        session.removeAttribute("apiRegisterCode");
        session.removeAttribute("apiRegisterEmail");
        session.removeAttribute("apiRegisterCodeTime");

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        userService.registerUser(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "注册成功"));
    }

    /** GET /api/auth/me — 获取当前登录用户信息 */
    @GetMapping("/auth/me")
    public ResponseEntity<Map<String, Object>> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Map<String, Object> result = buildUserMap(user);
        Long userId = user.getId();
        result.put("checkinCount", galleryUnlockRepository.countByUserId(userId));
        result.put("visitedShopCount", shopVisitRepository.countByUserId(userId));
        result.put("favCount", userService.countUserFavorites(userId));
        result.put("postCount", postService.countByUser(userId));
        return ResponseEntity.ok(result);
    }

    /** POST /api/auth/update-profile — 更新个人资料 */
    @PostMapping("/auth/update-profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        try {
            String name = (String) body.get("name");
            String phone = (String) body.get("phone");
            Integer gender = body.get("gender") != null ? ((Number) body.get("gender")).intValue() : null;
            Integer age = body.get("age") != null ? ((Number) body.get("age")).intValue() : null;
            String bio = (String) body.get("bio");
            userService.updateProfile(principal.getName(), name, phone, gender, age, bio);
            return ResponseEntity.ok(Map.of("success", true, "message", "资料修改成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/auth/change-password — 修改密码 */
    @PostMapping("/auth/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @RequestBody Map<String, String> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (newPwd == null || newPwd.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "新密码不能少于6位"));
        }
        try {
            userService.changePassword(principal.getName(), oldPwd, newPwd);
            return ResponseEntity.ok(Map.of("success", true, "message", "密码修改成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/auth/upload-avatar — 上传头像（multipart） */
    @PostMapping("/auth/upload-avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        try {
            if (file.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "请选择图片"));
            String ct = file.getContentType();
            if (ct == null || !ct.startsWith("image/"))
                return ResponseEntity.badRequest().body(Map.of("error", "只支持图片文件"));

            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String dirPath = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize().toString();
            File dir = new File(dirPath);
            if (!dir.exists()) dir.mkdirs();
            String orig = file.getOriginalFilename();
            String ext = (orig != null && orig.contains(".")) ? orig.substring(orig.lastIndexOf(".") + 1) : "jpg";
            String filename = user.getId() + "_" + System.currentTimeMillis() + "." + ext;
            File dest = new File(dirPath, filename);
            Files.write(dest.toPath(), file.getBytes());
            String avatarUrl = "/uploads/avatars/" + filename;

            user.setAvatarUrl(avatarUrl);
            userService.saveUser(user);
            return ResponseEntity.ok(Map.of("success", true, "avatarUrl", avatarUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "上传失败：" + e.getMessage()));
        }
    }

    /** POST /api/auth/logout — 登出（清除 Session 即可，Token 由客户端丢弃） */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // ==================== Forgot Password ====================

    /** POST /api/auth/forgot-password — 发送重置密码验证码 */
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入邮箱"));
        }
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "该邮箱未注册"));
        }
        // 生成 6 位验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        session.setAttribute("apiResetCode", code);
        session.setAttribute("apiResetEmail", email);
        session.setAttribute("apiResetCodeTime", System.currentTimeMillis());
        try {
            emailService.sendPasswordResetCode(email, code);
            return ResponseEntity.ok(Map.of("success", true, "message", "验证码已发送到邮箱"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "邮件发送失败，请稍后重试"));
        }
    }

    /** POST /api/auth/verify-reset-code — 验证重置密码验证码 */
    @PostMapping("/auth/verify-reset-code")
    public ResponseEntity<Map<String, Object>> verifyResetCode(
            @RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入验证码"));
        }
        String sessionEmail = (String) session.getAttribute("apiResetEmail");
        String sessionCode = (String) session.getAttribute("apiResetCode");
        Long codeTime = (Long) session.getAttribute("apiResetCodeTime");
        if (sessionCode == null || codeTime == null || sessionEmail == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "请先获取验证码"));
        }
        if (!sessionEmail.equals(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "邮箱不匹配，请重新获取验证码"));
        }
        if (System.currentTimeMillis() - codeTime > CODE_EXPIRY_MINUTES * 60 * 1000L) {
            session.removeAttribute("apiResetCode");
            session.removeAttribute("apiResetEmail");
            session.removeAttribute("apiResetCodeTime");
            return ResponseEntity.badRequest().body(Map.of("error", "验证码已过期，请重新获取"));
        }
        if (!sessionCode.equals(code.trim())) {
            return ResponseEntity.badRequest().body(Map.of("error", "验证码错误"));
        }
        // 验证通过，清除 code 但保留 verified 标记
        session.removeAttribute("apiResetCode");
        session.removeAttribute("apiResetCodeTime");
        session.setAttribute("apiResetVerified", true);
        return ResponseEntity.ok(Map.of("success", true, "message", "验证成功"));
    }

    /** POST /api/auth/reset-password — 重置密码（需先验证验证码） */
    @PostMapping("/auth/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @RequestBody Map<String, String> body, HttpSession session) {
        Boolean verified = (Boolean) session.getAttribute("apiResetVerified");
        String email = (String) session.getAttribute("apiResetEmail");
        if (!Boolean.TRUE.equals(verified) || email == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "请先验证邮箱"));
        }
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码不能少于6位"));
        }
        try {
            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "用户不存在"));
            }
            userService.resetPassword(userOpt.get().getUsername(), newPassword);
            session.removeAttribute("apiResetEmail");
            session.removeAttribute("apiResetVerified");
            return ResponseEntity.ok(Map.of("success", true, "message", "密码重置成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== Register Verification Code ====================

    /** POST /api/auth/send-register-code — 发送注册验证码到邮箱 */
    @PostMapping("/auth/send-register-code")
    public ResponseEntity<Map<String, Object>> sendRegisterCode(
            @RequestBody Map<String, String> body, HttpSession session) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请输入邮箱"));
        }
        if (userService.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "该邮箱已被注册"));
        }
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        session.setAttribute("apiRegisterCode", code);
        session.setAttribute("apiRegisterEmail", email);
        session.setAttribute("apiRegisterCodeTime", System.currentTimeMillis());
        try {
            emailService.sendRegisterCode(email, code);
            return ResponseEntity.ok(Map.of("success", true, "message", "验证码已发送到邮箱"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "邮件发送失败，请稍后重试"));
        }
    }

    // ============================================================
    // MY SHOPS / SELLER APPLY
    // ============================================================

    /** GET /api/my-shops — 当前用户的店铺列表 */
    @GetMapping("/my-shops")
    public ResponseEntity<List<Map<String, Object>>> getMyShops(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        Long userId = getPrincipalUserId(principal);
        List<Shop> shops = shopService.getShopsByOwner(userId);
        List<Map<String, Object>> result = shops.stream()
                .map(shop -> buildShopMap(shop, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** POST /api/shop/apply — 提交商家入驻申请 */
    @PostMapping("/shop/apply")
    public ResponseEntity<Map<String, Object>> applyShop(
            @RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        try {
            User owner = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            Shop shop = new Shop();
            shop.setShopName((String) body.getOrDefault("shopName", ""));
            shop.setCategory((String) body.getOrDefault("category", ""));
            shop.setCity((String) body.getOrDefault("city", ""));
            shop.setDistrict((String) body.getOrDefault("district", ""));
            shop.setLocation((String) body.getOrDefault("location", ""));
            shop.setPhone((String) body.getOrDefault("phone", ""));
            shop.setIntroduction((String) body.getOrDefault("introduction", ""));

            if (shop.getShopName() == null || shop.getShopName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "店铺名称不能为空"));
            }

            shopService.applyShop(shop, owner);
            return ResponseEntity.ok(Map.of("success", true, "message", "入驻申请已提交，请等待管理员审核"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "申请失败：" + e.getMessage()));
        }
    }

    // ============================================================
    // ORDERS
    // ============================================================

    /** POST /api/orders/create — 创建订单 */
    @PostMapping("/orders/create")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        try {
            User user = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            Long shopId = ((Number) body.get("shopId")).longValue();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            String note = (String) body.getOrDefault("note", "");

            Order order = orderService.createOrder(user, shopId, items, note);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("orderId", order.getId());
            result.put("totalAmount", order.getTotalAmount());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/orders/my — 我的订单列表 */
    @GetMapping("/orders/my")
    public ResponseEntity<List<Map<String, Object>>> getMyOrders(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        Long userId = getPrincipalUserId(principal);
        List<Order> orders = orderService.getMyOrders(userId);
        List<Map<String, Object>> result = orders.stream()
                .map(this::buildOrderMap)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** GET /api/orders/{id} — 订单详情 */
    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrderDetail(
            @PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        try {
            Order order = orderService.getOrderDetail(id);
            Map<String, Object> result = buildOrderMap(order);
            List<Map<String, Object>> items = orderService.getOrderItems(id).stream()
                    .map(this::buildOrderItemMap)
                    .collect(Collectors.toList());
            result.put("items", items);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // REGIONS
    // ============================================================

    /** GET /api/regions — 省市区数据 */
    @GetMapping("/regions")
    public ResponseEntity<Map<String, Object>> getRegions() {
        try {
            // 从静态 JS 文件中加载省市区数据，去掉 JS 包装后返回 JSON
            InputStream is = getClass().getResourceAsStream("/static/js/region-data.js");
            if (is == null) return ResponseEntity.notFound().build();
            String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            is.close();
            // 去掉 const REGION_DATA = 前缀和末尾的 ; 以及注释行
            content = content.replaceAll("(?s)/\\*.*?\\*/", "").trim();
            content = content.replace("const REGION_DATA = ", "").replaceAll(";$", "").trim();
            @SuppressWarnings("unchecked")
            Map<String, Object> regions = new com.google.gson.Gson().fromJson(content, Map.class);
            return ResponseEntity.ok(regions);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "加载失败"));
        }
    }

    // ============================================================
    // MERCHANTS
    // ============================================================

    /** GET /api/merchants?keyword= — 商家列表 */
    @GetMapping("/merchants")
    public ResponseEntity<List<Map<String, Object>>> getMerchants(
            @RequestParam(value = "keyword", required = false) String keyword,
            Principal principal) {
        List<Shop> shops = (keyword != null && !keyword.isBlank())
                ? shopService.searchShops(keyword)
                : shopService.getAllShops();

        Long userId = getPrincipalUserId(principal);
        Set<Long> visitedShopIds = userId != null
                ? shopVisitRepository.findByUserId(userId).stream()
                        .map(ShopVisit::getShopId).collect(Collectors.toSet())
                : Collections.emptySet();

        List<Map<String, Object>> result = shops.stream()
                .map(shop -> buildShopMap(shop, visitedShopIds.contains(shop.getId())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** GET /api/merchants/{id} — 商家详情 + 菜品列表 */
    @GetMapping("/merchants/{id}")
    public ResponseEntity<Map<String, Object>> getMerchantDetail(
            @PathVariable Long id, Principal principal) {
        return shopService.findById(id).map(shop -> {
            Long userId = getPrincipalUserId(principal);
            List<Dish> dishes = shopService.getDishesByShopId(id);
            Set<Long> unlockedDishIds = userId != null
                    ? new HashSet<>(galleryUnlockRepository.findUnlockedDishIdsByUserId(userId))
                    : Collections.emptySet();
            boolean visited = userId != null && shopVisitRepository.existsByUserIdAndShopId(userId, id);

            Map<String, Object> result = buildShopMap(shop, visited);
            result.put("dishes", dishes.stream().map(dish -> {
                Map<String, Object> d = buildDishMap(dish);
                d.put("unlocked", unlockedDishIds.contains(dish.getId()));
                return d;
            }).collect(Collectors.toList()));
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/merchants/{id}/checkin — 店铺打卡 */
    @PostMapping("/merchants/{id}/checkin")
    public ResponseEntity<Map<String, Object>> checkinShop(
            @PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        Long userId = getPrincipalUserId(principal);
        if (shopVisitRepository.existsByUserIdAndShopId(userId, id)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "您已打卡过此店铺"));
        }
        ShopVisit visit = new ShopVisit(userId, id);
        shopVisitRepository.save(visit);
        return ResponseEntity.ok(Map.of("success", true, "message", "店铺打卡成功"));
    }

    // ============================================================
    // GALLERY
    // ============================================================

    /** GET /api/gallery/cities — 城市列表 */
    @GetMapping("/gallery/cities")
    public ResponseEntity<List<Map<String, Object>>> getGalleryCities(Principal principal) {
        Long userId = getPrincipalUserId(principal);
        List<GalleryService.CityView> cities = galleryService.getCities(userId);
        List<Map<String, Object>> result = cities.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", c.getName());
            m.put("districtCount", c.getDistrictCount());
            m.put("shopCount", c.getShopCount());
            m.put("totalDishes", c.getTotalDishes());
            m.put("unlockedDishes", c.getUnlockedDishes());
            m.put("progress", c.getProgress());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** GET /api/gallery/districts?city= — 区域列表 */
    @GetMapping("/gallery/districts")
    public ResponseEntity<Map<String, Object>> getGalleryDistricts(
            @RequestParam String city, Principal principal) {
        Long userId = getPrincipalUserId(principal);
        GalleryService.DistrictPageView view = galleryService.getDistricts(userId, city);
        List<Map<String, Object>> districts = view.getDistricts().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", d.getName());
            m.put("shopCount", d.getShopCount());
            m.put("totalDishes", d.getTotalDishes());
            m.put("unlockedDishes", d.getUnlockedDishes());
            m.put("progress", d.getProgress());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("city", city, "districts", districts));
    }

    /** GET /api/gallery/shops?city=&district= — 店铺列表（图鉴级） */
    @GetMapping("/gallery/shops")
    public ResponseEntity<Map<String, Object>> getGalleryShops(
            @RequestParam String city, @RequestParam String district, Principal principal) {
        Long userId = getPrincipalUserId(principal);
        GalleryService.ShopPageView view = galleryService.getShops(userId, city, district);
        List<Map<String, Object>> shops = view.getShops().stream().map(sv -> {
            Map<String, Object> m = buildShopMap(sv.getShop(), false);
            m.put("totalDishes", sv.getTotalDishes());
            m.put("unlockedDishes", sv.getUnlockedDishes());
            m.put("progress", sv.getProgress());
            m.put("closed", sv.isClosed());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("city", city, "district", district, "shops", shops));
    }

    /** GET /api/gallery/dishes?shopId= — 菜品列表（图鉴级） */
    @GetMapping("/gallery/dishes")
    public ResponseEntity<Map<String, Object>> getGalleryDishes(
            @RequestParam Long shopId, Principal principal) {
        Long userId = getPrincipalUserId(principal);
        GalleryService.DishPageView view = galleryService.getDishes(userId, shopId);
        Map<String, Object> shopMap = buildShopMap(view.getShop(), false);
        List<Map<String, Object>> dishes = view.getDishes().stream().map(dv -> {
            Map<String, Object> m = buildDishMap(dv.getDish());
            m.put("unlocked", dv.isUnlocked());
            m.put("unavailable", dv.isUnavailable());
            m.put("displayImageUrl", dv.getImageUrl());
            m.put("checkinComment", dv.getComment());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("shop", shopMap, "dishes", dishes));
    }

    /** GET /api/gallery/stats — 全局图鉴统计 */
    @GetMapping("/gallery/stats")
    public ResponseEntity<Map<String, Object>> getGalleryStats(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        Long userId = getPrincipalUserId(principal);
        GalleryService.GlobalStats stats = galleryService.getGlobalStats(userId);
        return ResponseEntity.ok(Map.of(
                "totalDishes", stats.getTotalDishes(),
                "unlockedDishes", stats.getUnlockedDishes(),
                "progress", stats.getProgress()));
    }

    /** GET /api/gallery/search?keyword= — 图鉴搜索 */
    @GetMapping("/gallery/search")
    public ResponseEntity<List<Map<String, Object>>> searchGallery(
            @RequestParam String keyword, Principal principal) {
        Long userId = getPrincipalUserId(principal);
        List<GalleryService.SearchResultView> results = galleryService.search(userId, keyword);
        List<Map<String, Object>> out = results.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("shop", buildShopMap(r.getShop(), false));
            m.put("dishes", r.getDishes().stream().map(dv -> {
                Map<String, Object> d = buildDishMap(dv.getDish());
                d.put("unlocked", dv.isUnlocked());
                d.put("displayImageUrl", dv.getImageUrl());
                d.put("checkinComment", dv.getComment());
                return d;
            }).collect(Collectors.toList()));
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(out);
    }

    /** POST /api/gallery/unlock — 解锁菜品（含打卡图片 base64） */
    @PostMapping("/gallery/unlock")
    public ResponseEntity<Map<String, Object>> unlockDish(
            @RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        Long userId = getPrincipalUserId(principal);
        Long dishId = ((Number) body.get("dishId")).longValue();
        String base64Image = (String) body.get("image");
        String comment = (String) body.get("comment");

        String imageUrl = null;
        if (base64Image != null && !base64Image.isBlank()) {
            try {
                imageUrl = saveCheckinImage(base64Image, userId);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "图片保存失败：" + e.getMessage()));
            }
        }
        boolean newlyUnlocked = galleryService.unlockDish(userId, dishId, imageUrl, comment);
        String msg = newlyUnlocked ? "解锁成功！图鉴已更新" : "该菜品已经解锁过了";
        return ResponseEntity.ok(Map.of("success", true, "newlyUnlocked", newlyUnlocked, "message", msg));
    }

    // ============================================================
    // POSTS
    // ============================================================

    /** GET /api/posts?keyword=&tab=&page=&size= — 帖子列表 */
    @GetMapping("/posts")
    public ResponseEntity<Map<String, Object>> getPosts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tab", defaultValue = "all") String tab,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Principal principal) {
        Long userId = getPrincipalUserId(principal);

        List<PostService.PostView> postViews;
        long total;

        if ("favorites".equals(tab) && userId != null) {
            List<PostService.PostView> favs = postService.getMyFavorites(userId);
            total = favs.size();
            int from = Math.min(page * size, favs.size());
            int to = Math.min(from + size, favs.size());
            postViews = favs.subList(from, to);
        } else if ("my".equals(tab) && userId != null) {
            List<PostService.PostView> my = postService.getPostsByUser(userId, userId);
            total = my.size();
            int from = Math.min(page * size, my.size());
            int to = Math.min(from + size, my.size());
            postViews = my.subList(from, to);
        } else if (keyword != null && !keyword.isBlank()) {
            var paged = postService.searchPosts(keyword, page, size, userId);
            postViews = paged.getContent();
            total = paged.getTotalElements();
        } else {
            var paged = postService.getAllPosts(page, size, userId);
            postViews = paged.getContent();
            total = paged.getTotalElements();
        }

        List<Map<String, Object>> items = postViews.stream()
                .map(pv -> buildPostMap(pv, false))
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("posts", items, "total", total, "page", page, "size", size));
    }

    /** GET /api/posts/{id} — 帖子详情 + 评论 */
    @GetMapping("/posts/{id}")
    public ResponseEntity<Map<String, Object>> getPostDetail(
            @PathVariable Long id, Principal principal) {
        Long userId = getPrincipalUserId(principal);
        return postService.findById(id).map(post -> {
            boolean liked = userId != null && postService.isPostLiked(id, userId);
            boolean favorited = userId != null && postService.isPostFavorited(id, userId);
            PostService.PostView pv = new PostService.PostView(post, liked, favorited);
            Map<String, Object> result = buildPostMap(pv, true);
            List<Comment> comments = postService.getComments(id);
            result.put("comments", comments.stream().map(c -> {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("id", c.getId());
                cm.put("content", c.getContent());
                cm.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
                cm.put("authorId", c.getAuthor() != null ? c.getAuthor().getId() : null);
                cm.put("authorName", c.getAuthor() != null
                        ? (c.getAuthor().getName() != null ? c.getAuthor().getName() : c.getAuthor().getUsername())
                        : "未知");
                cm.put("authorAvatarUrl", c.getAuthor() != null ? c.getAuthor().getAvatarUrl() : null);
                return cm;
            }).collect(Collectors.toList()));
            return ResponseEntity.ok(result);
        }).orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/posts/create — 发帖（JSON，含 base64 图片列表） */
    @PostMapping("/posts/create")
    public ResponseEntity<Map<String, Object>> createPost(
            @RequestBody Map<String, Object> body, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        Long userId = getPrincipalUserId(principal);
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String location = (String) body.get("location");

        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "标题不能为空"));
        }

        // 处理 base64 图片列表
        @SuppressWarnings("unchecked")
        List<String> base64Images = (List<String>) body.get("images");
        List<String> imageUrls = new ArrayList<>();
        if (base64Images != null) {
            for (String b64 : base64Images) {
                try {
                    String url = savePostImage(b64, userId);
                    imageUrls.add(url);
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "图片保存失败：" + e.getMessage()));
                }
            }
        }

        Post post = postService.createPost(userId, title, content,
                imageUrls.isEmpty() ? null : String.join(",", imageUrls), location);
        return ResponseEntity.ok(Map.of("success", true, "postId", post.getId(),
                "message", "发帖成功，等待审核"));
    }

    /** POST /api/posts/{id}/like — 切换点赞 */
    @PostMapping("/posts/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(
            @PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        boolean liked = postService.toggleLike(id, getPrincipalUserId(principal));
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    /** POST /api/posts/{id}/favorite — 切换收藏 */
    @PostMapping("/posts/{id}/favorite")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        boolean favorited = postService.toggleFavorite(id, getPrincipalUserId(principal));
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    /** POST /api/posts/{id}/share — 分享（增加分享计数） */
    @PostMapping("/posts/{id}/share")
    public ResponseEntity<Map<String, Object>> sharePost(
            @PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        postService.incrementShareCount(id);
        return ResponseEntity.ok(Map.of("success", true));
    }

    /** POST /api/posts/{id}/comment — 提交评论 */
    @PostMapping("/posts/{id}/comment")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "评论内容不能为空"));
        }
        Comment comment = postService.addComment(id, getPrincipalUserId(principal), content);
        return ResponseEntity.ok(Map.of("success", true, "commentId", comment.getId()));
    }

    /** POST /api/posts/{id}/comment/delete — 删除评论（仅评论作者可删除） */
    @PostMapping("/posts/{id}/comment/delete")
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        Long commentId = ((Number) body.get("commentId")).longValue();
        try {
            postService.deleteComment(commentId, getPrincipalUserId(principal));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/posts/{id}/delete — 删除帖子 */
    @PostMapping("/posts/{id}/delete")
    public ResponseEntity<Map<String, Object>> deletePost(
            @PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        try {
            postService.deletePost(id, getPrincipalUserId(principal));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    private Long getPrincipalUserId(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName())
                .map(User::getId).orElse(null);
    }

    private Map<String, Object> buildUserMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", user.getId());
        m.put("username", user.getUsername());
        m.put("email", user.getEmail());
        m.put("name", user.getName());
        m.put("phone", user.getPhone());
        m.put("gender", user.getGender());
        m.put("age", user.getAge());
        m.put("bio", user.getBio());
        m.put("avatarUrl", user.getAvatarUrl());
        m.put("role", user.getRole() != null ? user.getRole().name() : "USER");
        return m;
    }

    private Map<String, Object> buildShopMap(Shop shop, boolean visited) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", shop.getId());
        m.put("shopName", shop.getShopName());
        m.put("introduction", shop.getIntroduction());
        m.put("location", shop.getLocation());
        m.put("city", shop.getCity());
        m.put("district", shop.getDistrict());
        m.put("category", shop.getCategory());
        m.put("phone", shop.getPhone());
        m.put("startTime", shop.getStartTime());
        m.put("endTime", shop.getEndTime());
        m.put("coverUrl", shop.getCoverUrl());
        m.put("status", shop.getStatus() != null ? shop.getStatus().name() : null);
        m.put("visited", visited);
        m.put("createdAt", shop.getCreatedAt() != null ? shop.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> buildDishMap(Dish dish) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", dish.getId());
        m.put("dishName", dish.getDishName());
        m.put("price", dish.getPrice());
        m.put("description", dish.getDescription());
        m.put("imageUrl", dish.getImageUrl());
        m.put("status", dish.getStatus() != null ? dish.getStatus().name() : null);
        m.put("shopId", dish.getShop() != null ? dish.getShop().getId() : null);
        return m;
    }

    private Map<String, Object> buildOrderMap(Order order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", order.getId());
        m.put("shopId", order.getShop() != null ? order.getShop().getId() : null);
        m.put("shopName", order.getShop() != null ? order.getShop().getShopName() : null);
        m.put("totalAmount", order.getTotalAmount());
        m.put("status", order.getStatus() != null ? order.getStatus().name() : null);
        m.put("note", order.getNote());
        m.put("createdAt", order.getCreatedAt() != null ? order.getCreatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> buildOrderItemMap(OrderItem item) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", item.getId());
        m.put("dishId", item.getDishId());
        m.put("dishName", item.getDishName());
        m.put("price", item.getPrice());
        m.put("quantity", item.getQuantity());
        return m;
    }

    private Map<String, Object> buildPostMap(PostService.PostView pv, boolean withFullContent) {
        Post post = pv.getPost();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", post.getId());
        m.put("title", post.getTitle());
        m.put("content", withFullContent ? post.getContent() : pv.getPreviewContent());
        m.put("location", post.getLocation());
        m.put("likeCount", post.getLikeCount());
        m.put("commentCount", post.getCommentCount());
        m.put("favoriteCount", post.getFavoriteCount());
        m.put("shareCount", post.getShareCount());
        m.put("liked", pv.isLiked());
        m.put("favorited", pv.isFavorited());
        m.put("createdAt", post.getCreatedAt() != null ? post.getCreatedAt().toString() : null);
        m.put("images", pv.getImageList());
        m.put("authorId", post.getAuthor() != null ? post.getAuthor().getId() : null);
        m.put("authorName", pv.getAuthorName());
        m.put("authorAvatarUrl", pv.getAuthorAvatarUrl());
        m.put("status", post.getStatus() != null ? post.getStatus().name() : null);
        return m;
    }

    private String saveCheckinImage(String base64Data, Long userId) throws Exception {
        String data = base64Data.contains(",") ? base64Data.substring(base64Data.indexOf(",") + 1) : base64Data;
        byte[] bytes = Base64.getDecoder().decode(data);
        String dirPath = uploadDir + "/checkin/";
        new File(dirPath).mkdirs();
        String filename = userId + "_" + System.currentTimeMillis() + ".jpg";
        try (FileOutputStream fos = new FileOutputStream(dirPath + filename)) {
            fos.write(bytes);
        }
        return "/uploads/checkin/" + filename;
    }

    private String savePostImage(String base64Data, Long userId) throws Exception {
        String data = base64Data.contains(",") ? base64Data.substring(base64Data.indexOf(",") + 1) : base64Data;
        byte[] bytes = Base64.getDecoder().decode(data);
        String dirPath = uploadDir + "/posts/";
        new File(dirPath).mkdirs();
        String filename = userId + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + ".jpg";
        try (FileOutputStream fos = new FileOutputStream(dirPath + filename)) {
            fos.write(bytes);
        }
        return "/uploads/posts/" + filename;
    }
}
