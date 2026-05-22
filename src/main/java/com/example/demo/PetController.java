package com.example.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/pet")
public class PetController {

    private final UserService userService;
    private final PetService petService;

    public PetController(UserService userService, PetService petService) {
        this.userService = userService;
        this.petService = petService;
    }

    private Long getUserId(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName()).map(User::getId).orElse(null);
    }

    // ==================== 宠物主页 ====================

    @GetMapping("")
    public String showPet(Model model, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        PetService.PetView petView = petService.getPetView(userId);
        model.addAttribute("pet", petView.pet);
        model.addAttribute("points", petView.points);
        return "pet";
    }

    // ==================== 改名（15天冷却） ====================

    @PostMapping("/rename")
    public String rename(@RequestParam("nickname") String nickname,
                         Principal principal, RedirectAttributes ra) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        PetService.RenameResult result = petService.renamePet(userId, nickname);
        ra.addFlashAttribute(result.success ? "success" : "error", result.message);
        return "redirect:/pet";
    }

    // ==================== 更换形象 ====================

    /** 首次免费选择形象 */
    @PostMapping("/select-type")
    public String selectType(@RequestParam("petType") String petType,
                             Principal principal, RedirectAttributes ra) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        PetService.TypeChangeResult result = petService.selectPetType(userId, petType);
        ra.addFlashAttribute(result.success ? "success" : "error", result.message);
        return "redirect:/pet";
    }

    /** 消耗积分更换形象 */
    @PostMapping("/change-type")
    public String changeType(@RequestParam("petType") String petType,
                             Principal principal, RedirectAttributes ra) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        PetService.TypeChangeResult result = petService.changePetType(userId, petType, false);
        ra.addFlashAttribute(result.success ? "success" : "error", result.message);
        return "redirect:/pet";
    }

    // ==================== 宠物商店 ====================

    @GetMapping("/shop")
    public String showShop(Model model, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        List<PetShopItem> items = petService.getShopItems();
        User user = userService.findById(userId).orElse(null);
        model.addAttribute("items", items);
        model.addAttribute("points", user != null ? user.getPetPoints() : 0);
        return "pet-shop";
    }

    @PostMapping("/shop/buy")
    public String buyItem(@RequestParam("itemId") Long itemId,
                          Principal principal, RedirectAttributes ra) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        PetService.BuyResult result = petService.buyItem(userId, itemId);
        ra.addFlashAttribute(result.success ? "success" : "error", result.message);
        return "redirect:/pet/shop";
    }

    // ==================== 背包 ====================

    @GetMapping("/inventory")
    public String showInventory(Model model, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        List<PetService.InventoryView> inventory = petService.getInventory(userId);
        User user = userService.findById(userId).orElse(null);
        model.addAttribute("inventory", inventory);
        model.addAttribute("points", user != null ? user.getPetPoints() : 0);
        return "pet-inventory";
    }

    @PostMapping("/use/{inventoryId}")
    public String useItem(@PathVariable Long inventoryId,
                          Principal principal, RedirectAttributes ra) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";

        PetService.UseResult result = petService.useItem(userId, inventoryId);
        ra.addFlashAttribute(result.success ? "success" : "error", result.message);
        return "redirect:/pet/inventory";
    }
}
