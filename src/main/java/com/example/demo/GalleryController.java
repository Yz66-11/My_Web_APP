package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.Optional;

@Controller
public class GalleryController {

    private final GalleryService galleryService;
    private final UserService userService;

    public GalleryController(GalleryService galleryService, UserService userService) {
        this.galleryService = galleryService;
        this.userService = userService;
    }

    private Long getUserId(Principal principal) {
        if (principal == null) return null;
        Optional<User> userOpt = userService.findByUsername(principal.getName());
        return userOpt.map(User::getId).orElse(null);
    }

    @GetMapping("/food-gallery")
    public String showGallery(
            @RequestParam(value = "city", required = false) String city,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "shopId", required = false) Long shopId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "filter", required = false) String filter,
            Model model, Principal principal) {

        Long userId = getUserId(principal);
        model.addAttribute("keyword", keyword);
        model.addAttribute("filter", filter != null ? filter : "all");

        // Search mode
        if (keyword != null && !keyword.trim().isEmpty()) {
            model.addAttribute("searchResults", galleryService.search(userId, keyword));
            model.addAttribute("isSearch", true);
            model.addAttribute("breadcrumb", "搜索: " + keyword);
            return "food-gallery";
        }

        // Drill-down hierarchy
        if (shopId != null) {
            model.addAttribute("dishPage", galleryService.getDishes(userId, shopId));
            model.addAttribute("breadcrumb",
                    city + " / " + district + " / " +
                    galleryService.getDishes(userId, shopId).getShop().getShopName());
            model.addAttribute("level", "dish");
            model.addAttribute("city", city);
            model.addAttribute("district", district);
        } else if (district != null && city != null) {
            model.addAttribute("shopPage", galleryService.getShops(userId, city, district));
            model.addAttribute("breadcrumb", city + " / " + district);
            model.addAttribute("level", "shop");
            model.addAttribute("city", city);
        } else if (city != null) {
            model.addAttribute("districtPage", galleryService.getDistricts(userId, city));
            model.addAttribute("breadcrumb", city);
            model.addAttribute("level", "district");
        } else {
            model.addAttribute("cities", galleryService.getCities(userId));
            model.addAttribute("breadcrumb", "全部城市");
            model.addAttribute("level", "city");
        }

        // Global stats
        GalleryService.GlobalStats stats = galleryService.getGlobalStats(userId);
        model.addAttribute("stats", stats);

        model.addAttribute("isSearch", false);
        return "food-gallery";
    }

    @PostMapping("/food-gallery/unlock")
    public String unlockDish(@RequestParam("dishId") Long dishId,
                              @RequestParam(value = "city", required = false) String city,
                              @RequestParam(value = "district", required = false) String district,
                              @RequestParam(value = "shopId", required = false) Long shopId,
                              RedirectAttributes redirectAttributes,
                              Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) {
            return "redirect:/login";
        }
        try {
            boolean newlyUnlocked = galleryService.unlockDish(userId, dishId);
            if (newlyUnlocked) {
                redirectAttributes.addFlashAttribute("success", "解锁成功！新菜品已加入你的美食图鉴");
            } else {
                redirectAttributes.addFlashAttribute("info", "该菜品已经解锁过了");
            }
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "解锁失败: " + e.getMessage());
        }

        StringBuilder redirect = new StringBuilder("redirect:/food-gallery?shopId=").append(shopId);
        if (city != null) redirect.append("&city=").append(city);
        if (district != null) redirect.append("&district=").append(district);
        return redirect.toString();
    }
}
