package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class MerchantController {

    private final ShopService shopService;
    private final UserService userService;

    public MerchantController(ShopService shopService, UserService userService) {
        this.shopService = shopService;
        this.userService = userService;
    }

    @GetMapping("/merchants")
    public String showMerchants(@RequestParam(value = "keyword", required = false) String keyword,
                                Model model) {
        var shops = (keyword != null && !keyword.trim().isEmpty())
                ? shopService.searchShops(keyword)
                : shopService.getAllShops();
        model.addAttribute("merchants", shops);
        model.addAttribute("keyword", keyword);
        return "merchants";
    }

    @GetMapping("/merchant/{id}")
    public String showMerchantDetail(@PathVariable Long id, Model model) {
        Optional<Shop> shopOpt = shopService.findById(id);
        if (shopOpt.isEmpty()) {
            return "redirect:/merchants";
        }
        Shop shop = shopOpt.get();
        model.addAttribute("shop", shop);
        model.addAttribute("dishes", shopService.getDishesByShopId(id));
        return "merchant-detail";
    }

    @GetMapping("/shop/apply")
    public String showApplyForm(Model model) {
        model.addAttribute("shop", new Shop());
        return "shop-apply";
    }

    @PostMapping("/shop/apply")
    public String processApply(@ModelAttribute Shop shop, Principal principal,
                               RedirectAttributes redirectAttributes) {
        try {
            User owner = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            Shop savedShop = shopService.applyShop(shop, owner);
            redirectAttributes.addFlashAttribute("success",
                    "入驻申请已提交，请等待管理员审核！您现在可以预先添加菜品。");
            return "redirect:/merchant/" + savedShop.getId() + "/dishes";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/shop/apply";
        }
    }

    @GetMapping("/my-shops")
    public String showMyShops(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user != null) {
            model.addAttribute("shops", shopService.getShopsByOwner(user.getId()));
        }
        return "my-shops";
    }

    @GetMapping("/shop/{id}/edit")
    public String showEditShop(@PathVariable Long id, Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        Optional<Shop> shopOpt = shopService.findById(id);
        if (shopOpt.isEmpty()) {
            return "redirect:/my-shops";
        }
        Shop shop = shopOpt.get();

        boolean isOwner = shop.getOwner() != null && shop.getOwner().getId().equals(user.getId());
        if (!isOwner && !user.isAdmin()) {
            return "redirect:/my-shops";
        }

        model.addAttribute("shop", shop);
        return "shop-edit";
    }

    @PostMapping("/shop/{id}/edit")
    public String processEditShop(@PathVariable Long id,
                                   @RequestParam String shopName,
                                   @RequestParam String category,
                                   @RequestParam String city,
                                   @RequestParam String district,
                                   @RequestParam String location,
                                   @RequestParam String phone,
                                   @RequestParam(required = false) String startTime,
                                   @RequestParam(required = false) String endTime,
                                   @RequestParam(required = false) String introduction,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        verifyOwnership(id, principal);
        shopService.updateShop(id, shopName, category, city, district, location, phone, startTime, endTime, introduction);
        redirectAttributes.addFlashAttribute("success", "店铺信息更新成功");
        return "redirect:/my-shops";
    }

    /* ==================== 菜品管理 ==================== */

    @GetMapping("/merchant/{shopId}/dishes")
    public String showDishManagement(@PathVariable Long shopId, Model model,
                                     Principal principal) {
        User user = userService.findByUsername(principal.getName()).orElse(null);
        if (user == null) return "redirect:/login";

        Optional<Shop> shopOpt = shopService.findById(shopId);
        if (shopOpt.isEmpty()) {
            return "redirect:/my-shops";
        }
        Shop shop = shopOpt.get();

        // 验证权限：只有店主或管理员可以管理
        boolean isOwner = shop.getOwner() != null && shop.getOwner().getId().equals(user.getId());
        if (!isOwner && !user.isAdmin()) {
            return "redirect:/my-shops?error=" +
                    java.net.URLEncoder.encode("无权操作该店铺", java.nio.charset.StandardCharsets.UTF_8);
        }

        List<Dish> dishes = shopService.getDishesByShopId(shopId);
        model.addAttribute("shop", shop);
        model.addAttribute("dishes", dishes);
        model.addAttribute("editMode", false);
        return "merchant-dishes";
    }

    @PostMapping("/merchant/{shopId}/dishes")
    public String addDish(@PathVariable Long shopId,
                          @RequestParam String dishName,
                          @RequestParam BigDecimal price,
                          @RequestParam(required = false) String description,
                          @RequestParam(required = false) String imageUrl,
                          Principal principal,
                          RedirectAttributes redirectAttributes) {
        verifyOwnership(shopId, principal);

        Dish dish = new Dish();
        dish.setDishName(dishName);
        dish.setPrice(price);
        dish.setDescription(description);
        dish.setImageUrl(imageUrl);
        shopService.addDish(dish, shopId);

        redirectAttributes.addFlashAttribute("success", "菜品「" + dishName + "」添加成功");
        return "redirect:/merchant/" + shopId + "/dishes";
    }

    @GetMapping("/merchant/{shopId}/dishes/{dishId}/edit")
    public String showEditDish(@PathVariable Long shopId, @PathVariable Long dishId,
                                Model model, Principal principal) {
        verifyOwnership(shopId, principal);

        Optional<Dish> dishOpt = shopService.getDishById(dishId);
        if (dishOpt.isEmpty()) {
            return "redirect:/merchant/" + shopId + "/dishes";
        }

        Optional<Shop> shopOpt = shopService.findById(shopId);
        model.addAttribute("shop", shopOpt.orElse(null));
        model.addAttribute("dish", dishOpt.get());
        model.addAttribute("editMode", true);
        return "merchant-dishes";
    }

    @PostMapping("/merchant/{shopId}/dishes/{dishId}/update")
    public String updateDish(@PathVariable Long shopId, @PathVariable Long dishId,
                             @RequestParam String dishName,
                             @RequestParam BigDecimal price,
                             @RequestParam(required = false) String description,
                             @RequestParam(required = false) String imageUrl,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        verifyOwnership(shopId, principal);
        shopService.updateDish(dishId, dishName, price, description, imageUrl);

        redirectAttributes.addFlashAttribute("success", "菜品信息更新成功");
        return "redirect:/merchant/" + shopId + "/dishes";
    }

    @PostMapping("/merchant/{shopId}/dishes/{dishId}/delete")
    public String deleteDish(@PathVariable Long shopId, @PathVariable Long dishId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        verifyOwnership(shopId, principal);
        shopService.deleteDish(dishId);

        redirectAttributes.addFlashAttribute("success", "菜品已删除");
        return "redirect:/merchant/" + shopId + "/dishes";
    }

    @PostMapping("/merchant/{shopId}/dishes/{dishId}/toggle")
    public String toggleDishStatus(@PathVariable Long shopId, @PathVariable Long dishId,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        verifyOwnership(shopId, principal);
        Dish dish = shopService.toggleDishStatus(dishId);
        String statusText = dish.getStatus() == Dish.DishStatus.AVAILABLE ? "上架" : "下架";

        redirectAttributes.addFlashAttribute("success",
                "菜品「" + dish.getDishName() + "」已" + statusText);
        return "redirect:/merchant/" + shopId + "/dishes";
    }

    /**
     * 验证当前用户是否为该店铺的店主
     */
    private void verifyOwnership(Long shopId, Principal principal) {
        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("用户未登录"));
        Shop shop = shopService.findById(shopId)
                .orElseThrow(() -> new RuntimeException("店铺不存在"));
        if (shop.getOwner() == null || !shop.getOwner().getId().equals(user.getId())) {
            throw new RuntimeException("无权操作此店铺的菜品");
        }
    }
}
