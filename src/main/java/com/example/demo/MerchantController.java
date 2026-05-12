package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.security.Principal;
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
    public String processApply(@ModelAttribute Shop shop, Principal principal) {
        try {
            User owner = userService.findByUsername(principal.getName())
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            shopService.applyShop(shop, owner);
            return "redirect:/shop/apply?success=" + java.net.URLEncoder.encode(
                    "入驻申请已提交，请等待管理员审核！", java.nio.charset.StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return "redirect:/shop/apply?error=" + java.net.URLEncoder.encode(
                    e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
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
}
