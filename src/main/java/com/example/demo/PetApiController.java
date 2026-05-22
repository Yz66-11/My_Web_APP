package com.example.demo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.*;

/**
 * REST API — Android 宠物养成接口
 */
@RestController
@RequestMapping("/api/pet")
public class PetApiController {

    private final UserService userService;
    private final PetService petService;

    public PetApiController(UserService userService, PetService petService) {
        this.userService = userService;
        this.petService = petService;
    }

    private Long getUserId(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName()).map(User::getId).orElse(null);
    }

    /** GET /api/pet/info — 宠物信息（宠物详情 + 积分） */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getPetInfo(Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));

        PetService.PetView pv = petService.getPetView(userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("points", pv.points);
        result.put("pet", buildPetMap(pv.pet));
        return ResponseEntity.ok(result);
    }

    /** POST /api/pet/rename — 宠物改名 */
    @PostMapping("/rename")
    public ResponseEntity<Map<String, Object>> rename(
            @RequestBody Map<String, String> body, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        String nickname = body.get("nickname");
        if (nickname == null || nickname.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "昵称不能为空"));
        }
        PetService.RenameResult r = petService.renamePet(userId, nickname.trim());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", r.success);
        result.put("message", r.message);
        if (r.pet != null) result.put("pet", buildPetMap(r.pet));
        return ResponseEntity.ok(result);
    }

    /** POST /api/pet/select-type — 首次免费选择形象 */
    @PostMapping("/select-type")
    public ResponseEntity<Map<String, Object>> selectType(
            @RequestBody Map<String, String> body, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        String petType = body.get("petType");
        if (petType == null) return ResponseEntity.badRequest().body(Map.of("error", "缺少类型参数"));
        PetService.TypeChangeResult r = petService.selectPetType(userId, petType.trim());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", r.success);
        result.put("message", r.message);
        result.put("cost", r.cost);
        if (r.pet != null) result.put("pet", buildPetMap(r.pet));
        return ResponseEntity.ok(result);
    }

    /** POST /api/pet/change-type — 消耗积分更换形象 */
    @PostMapping("/change-type")
    public ResponseEntity<Map<String, Object>> changeType(
            @RequestBody Map<String, String> body, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        String petType = body.get("petType");
        if (petType == null) return ResponseEntity.badRequest().body(Map.of("error", "缺少类型参数"));
        PetService.TypeChangeResult r = petService.changePetType(userId, petType.trim(), false);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", r.success);
        result.put("message", r.message);
        result.put("cost", r.cost);
        if (r.pet != null) result.put("pet", buildPetMap(r.pet));
        return ResponseEntity.ok(result);
    }

    /** GET /api/pet/shop — 道具商店列表 */
    @GetMapping("/shop")
    public ResponseEntity<Map<String, Object>> getShop(Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        List<PetShopItem> items = petService.getShopItems();
        User user = userService.findById(userId).orElse(null);
        List<Map<String, Object>> list = new ArrayList<>();
        for (PetShopItem item : items) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", item.getId());
            m.put("name", item.getName());
            m.put("price", item.getPrice());
            m.put("type", item.getType().name());
            m.put("effectKey", item.getEffectKey());
            m.put("effectValue", item.getEffectValue());
            m.put("iconUrl", item.getIconUrl());
            m.put("description", item.getDescription());
            list.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("points", user != null ? user.getPetPoints() : 0);
        result.put("items", list);
        return ResponseEntity.ok(result);
    }

    /** POST /api/pet/shop/buy — 购买道具 */
    @PostMapping("/shop/buy")
    public ResponseEntity<Map<String, Object>> buyItem(
            @RequestBody Map<String, Object> body, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        Long itemId = ((Number) body.get("itemId")).longValue();

        PetService.BuyResult r = petService.buyItem(userId, itemId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", r.success);
        result.put("message", r.message);
        result.put("remainingPoints", r.remainingPoints);
        if (r.inventory != null) {
            result.put("quantity", r.inventory.getQuantity());
        }
        return ResponseEntity.ok(result);
    }

    /** GET /api/pet/inventory — 背包道具列表 */
    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getInventory(Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        List<PetService.InventoryView> inv = petService.getInventory(userId);
        User user = userService.findById(userId).orElse(null);

        List<Map<String, Object>> list = new ArrayList<>();
        for (PetService.InventoryView iv : inv) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("inventoryId", iv.inventory.getId());
            m.put("itemId", iv.inventory.getItemId());
            m.put("itemName", iv.itemName);
            m.put("itemType", iv.itemType);
            m.put("quantity", iv.inventory.getQuantity());
            m.put("price", iv.price);
            m.put("effectKey", iv.item.getEffectKey());
            m.put("effectValue", iv.item.getEffectValue());
            list.add(m);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("points", user != null ? user.getPetPoints() : 0);
        result.put("items", list);
        return ResponseEntity.ok(result);
    }

    /** POST /api/pet/inventory/{id}/use — 使用道具 */
    @PostMapping("/inventory/{id}/use")
    public ResponseEntity<Map<String, Object>> useItem(
            @PathVariable Long id, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        PetService.UseResult r = petService.useItem(userId, id);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", r.success);
        result.put("message", r.message);
        if (r.pet != null) {
            result.put("pet", buildPetMap(r.pet));
        }
        return ResponseEntity.ok(result);
    }

    // ==================== Helper ====================

    private Map<String, Object> buildPetMap(UserPet pet) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", pet.getId());
        m.put("userId", pet.getUserId());
        m.put("petType", pet.getPetType());
        m.put("nickname", pet.getNickname());
        m.put("level", pet.getLevel());
        m.put("experience", pet.getExperience());
        m.put("expToNextLevel", pet.getExpToNextLevel());
        m.put("appearanceJson", pet.getAppearanceJson());
        m.put("petEmoji", pet.getPetEmoji());
        return m;
    }
}
