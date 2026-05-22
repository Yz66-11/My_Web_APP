package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 宠物养成核心服务。
 * - 注册自动领养宠物
 * - 打卡积分（Haversine 距离公式）
 * - 道具商店购买/使用/升级/装扮
 */
@Service
public class PetService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final int MIN_POINTS = 5;
    private static final int MAX_POINTS = 100;
    private static final int POINTS_PER_KM = 10;
    /** 每升一级所需经验增量 */
    private static final int EXP_PER_LEVEL = 100;
    /** 最大等级 */
    private static final int MAX_LEVEL = 50;
    /** 更换形象消耗积分 */
    public static final int TYPE_CHANGE_COST = 160;
    /** 改名冷却天数 */
    private static final int RENAME_COOLDOWN_DAYS = 15;
    /** 更换形象冷却天数 */
    private static final int TYPE_CHANGE_COOLDOWN_DAYS = 30;

    public static final String[] PET_TYPES = {"panda", "cat", "dog", "lion", "tiger"};
    private static final String[] DEFAULT_NICKNAMES = {"小可爱", "团团", "圆圆", "毛毛", "豆豆", "花花"};

    private final UserRepository userRepository;
    private final UserPetRepository petRepository;
    private final PetShopItemRepository shopItemRepository;
    private final UserPetInventoryRepository inventoryRepository;
    private final DishRepository dishRepository;

    public PetService(UserRepository userRepository, UserPetRepository petRepository,
                      PetShopItemRepository shopItemRepository,
                      UserPetInventoryRepository inventoryRepository,
                      DishRepository dishRepository) {
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.shopItemRepository = shopItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.dishRepository = dishRepository;
    }

    // ==================== 宠物授予 ====================

    @Transactional
    public UserPet grantPet(Long userId) {
        if (petRepository.existsByUserId(userId)) {
            return petRepository.findByUserId(userId).orElse(null);
        }
        // 默认熊猫
        String nickname = DEFAULT_NICKNAMES[new Random().nextInt(DEFAULT_NICKNAMES.length)];
        UserPet pet = new UserPet(userId, "panda", nickname);
        return petRepository.save(pet);
    }

    // ==================== 积分计算 ====================

    /**
     * Haversine 公式计算两点间的球面距离（公里）。
     */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * 根据用户与店铺距离计算打卡积分。
     * 公式：floor(距离km × 10)，最低 5 分，最高 100 分。
     */
    public static int calculatePoints(double distanceKm) {
        int points = (int) Math.floor(distanceKm * POINTS_PER_KM);
        return Math.max(MIN_POINTS, Math.min(MAX_POINTS, points));
    }

    /**
     * 根据用户坐标和店铺坐标计算积分。
     * 如果店铺没有坐标，返回最低积分。
     */
    public static int calculatePoints(double userLat, double userLng, Double shopLat, Double shopLng) {
        if (shopLat == null || shopLng == null) {
            return MIN_POINTS;
        }
        double dist = haversineKm(userLat, userLng, shopLat, shopLng);
        return calculatePoints(dist);
    }

    // ==================== 打卡奖励积分 ====================

    @Transactional
    public int awardCheckinPoints(Long userId, Long dishId, double userLat, double userLng) {
        // 通过菜品找到店铺，获取店铺坐标
        Dish dish = dishRepository.findById(dishId).orElse(null);
        if (dish == null || dish.getShop() == null) {
            return awardPoints(userId, MIN_POINTS);
        }
        Shop shop = dish.getShop();
        int points = calculatePoints(userLat, userLng, shop.getLatitude(), shop.getLongitude());
        return awardPoints(userId, points);
    }

    @Transactional
    public int awardCheckinPoints(Long userId, double userLat, double userLng, Shop shop) {
        int points = calculatePoints(userLat, userLng, shop.getLatitude(), shop.getLongitude());
        return awardPoints(userId, points);
    }

    /**
     * 直接给用户增加积分（底层的原子操作）。
     * @return 实际增加的积分
     */
    @Transactional
    public int awardPoints(Long userId, int points) {
        if (points <= 0) return 0;
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return 0;
        user.setPetPoints(user.getPetPoints() + points);
        userRepository.save(user);
        return points;
    }

    // ==================== 宠物查询 ====================

    public PetView getPetView(Long userId) {
        UserPet pet = getOrCreatePet(userId);
        User user = userRepository.findById(userId).orElse(null);
        int points = user != null ? user.getPetPoints() : 0;
        return new PetView(pet, points);
    }

    /** 容错：如果用户还没有宠物，自动补发 */
    private UserPet getOrCreatePet(Long userId) {
        return petRepository.findByUserId(userId)
                .orElseGet(() -> grantPet(userId));
    }

    // ==================== 道具商店 ====================

    public List<PetShopItem> getShopItems() {
        return shopItemRepository.findByActiveTrueOrderBySortOrderAsc();
    }

    public Optional<PetShopItem> getShopItem(Long itemId) {
        return shopItemRepository.findById(itemId);
    }

    // ==================== 购买道具 ====================

    @Transactional
    public BuyResult buyItem(Long userId, Long itemId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return BuyResult.fail("用户不存在");

        PetShopItem item = shopItemRepository.findById(itemId).orElse(null);
        if (item == null || !item.isActive()) return BuyResult.fail("道具不存在或已下架");

        if (user.getPetPoints() < item.getPrice()) {
            return BuyResult.fail("积分不足，需要 " + item.getPrice() + " 积分，当前 " + user.getPetPoints() + " 积分");
        }

        // 扣积分
        user.setPetPoints(user.getPetPoints() - item.getPrice());
        userRepository.save(user);

        // 加入背包
        UserPetInventory inv = inventoryRepository.findByUserIdAndItemId(userId, itemId)
                .orElse(null);
        if (inv == null) {
            inv = new UserPetInventory(userId, itemId, 1);
        } else {
            inv.setQuantity(inv.getQuantity() + 1);
        }
        inventoryRepository.save(inv);

        return BuyResult.success("购买成功！", user.getPetPoints(), inv);
    }

    // ==================== 使用道具 ====================

    @Transactional
    public UseResult useItem(Long userId, Long inventoryId) {
        UserPetInventory inv = inventoryRepository.findById(inventoryId).orElse(null);
        if (inv == null || !inv.getUserId().equals(userId)) {
            return UseResult.fail("背包中没有该道具");
        }
        if (inv.getQuantity() <= 0) {
            return UseResult.fail("道具数量不足");
        }

        PetShopItem item = shopItemRepository.findById(inv.getItemId()).orElse(null);
        if (item == null) return UseResult.fail("道具不存在");

        UserPet pet = getOrCreatePet(userId);
        String message;

        switch (item.getType()) {
            case FOOD:
                message = useFood(pet, item);
                break;
            case DECORATION:
                message = useDecoration(pet, item);
                break;
            case SPECIAL:
                message = useSpecial(pet, item);
                break;
            default:
                return UseResult.fail("未知道具类型");
        }

        // 减少/删除背包道具
        inv.setQuantity(inv.getQuantity() - 1);
        if (inv.getQuantity() <= 0) {
            inventoryRepository.delete(inv);
        } else {
            inventoryRepository.save(inv);
        }
        petRepository.save(pet);

        return UseResult.success(message, pet);
    }

    private String useFood(UserPet pet, PetShopItem item) {
        if (pet.getLevel() >= MAX_LEVEL) {
            return "宠物已达满级 " + MAX_LEVEL + " 级，使用食物也不会增加经验哦~";
        }
        int expGain = item.getEffectValue();
        pet.setExperience(pet.getExperience() + expGain);

        // 检查升级
        int leveled = 0;
        while (pet.getExperience() >= pet.getExpToNextLevel() && pet.getLevel() < MAX_LEVEL) {
            pet.setExperience(pet.getExperience() - pet.getExpToNextLevel());
            pet.setLevel(pet.getLevel() + 1);
            pet.setExpToNextLevel(pet.getLevel() * EXP_PER_LEVEL);
            leveled++;
        }
        StringBuilder sb = new StringBuilder("经验 +" + expGain);
        if (leveled > 0) {
            sb.append("，升级至 ").append(pet.getLevel()).append(" 级！");
        }
        return sb.toString();
    }

    private String useDecoration(UserPet pet, PetShopItem item) {
        String key = item.getEffectKey();   // 例如 "hat", "outfit", "accessory"
        String value = item.getName();       // 装扮名称
        if (key == null || key.isBlank()) return "装扮道具配置异常";

        // 更新装扮 JSON
        String json = pet.getAppearanceJson();
        if (json == null || json.isBlank()) {
            json = "{}";
        }
        // 简单方法：替换对应 key 的值（用字符串处理避免引入 JSON 库依赖）
        json = updateAppearanceKey(json, key, value);
        pet.setAppearanceJson(json);

        return "已为宠物穿上【" + value + "】！";
    }

    private String useSpecial(UserPet pet, PetShopItem item) {
        // 特殊道具：可以绽放等级特效等
        String key = item.getEffectKey();
        if ("rename".equals(key)) {
            return "请使用改名功能修改宠物昵称";
        }
        if ("effect".equals(key)) {
            String json = pet.getAppearanceJson();
            json = updateAppearanceKey(json, "effect", item.getName());
            pet.setAppearanceJson(json);
            return "已激活特效【" + item.getName() + "】！";
        }
        return "使用了【" + item.getName() + "】";
    }

    /**
     * 简单 JSON 字符串修改：找到 key 并替换 value。
     * 适用于 {"body":"default","hat":"none"...} 格式。
     */
    private String updateAppearanceKey(String json, String key, String value) {
        String searchStart = "\"" + key + "\":\"";
        int idx = json.indexOf(searchStart);
        if (idx < 0) {
            // 如果 key 不存在，添加到末尾
            if (json.endsWith("}")) {
                return json.substring(0, json.length() - 1) + ",\"" + key + "\":\"" + value + "\"}";
            }
            return json;
        }
        int valStart = idx + searchStart.length();
        int valEnd = json.indexOf("\"", valStart);
        if (valEnd < 0) return json;
        return json.substring(0, valStart) + value + json.substring(valEnd);
    }

    // ==================== 宠物改名（15天冷却） ====================

    @Transactional
    public RenameResult renamePet(Long userId, String newNickname) {
        UserPet pet = getOrCreatePet(userId);
        if (newNickname == null || newNickname.isBlank()) {
            return RenameResult.fail("昵称不能为空", pet);
        }
        // 冷却检查
        String cooldownMsg = checkRenameCooldown(pet);
        if (cooldownMsg != null) {
            return RenameResult.fail(cooldownMsg, pet);
        }
        pet.setNickname(newNickname.trim());
        pet.setLastRenameTime(LocalDateTime.now());
        petRepository.save(pet);
        return RenameResult.success("改名成功！", pet);
    }

    private String checkRenameCooldown(UserPet pet) {
        if (pet.getLastRenameTime() == null) return null;
        long days = ChronoUnit.DAYS.between(pet.getLastRenameTime(), LocalDateTime.now());
        if (days < RENAME_COOLDOWN_DAYS) {
            long remain = RENAME_COOLDOWN_DAYS - days;
            return "改名冷却中，还需等待 " + remain + " 天";
        }
        return null;
    }

    // ==================== 更换形象（30天冷却，160积分） ====================

    /**
     * @param free 是否免费（首次选择免费）
     */
    @Transactional
    public TypeChangeResult changePetType(Long userId, String newType, boolean free) {
        // 校验类型
        boolean valid = false;
        for (String t : PET_TYPES) {
            if (t.equals(newType)) { valid = true; break; }
        }
        if (!valid) {
            return TypeChangeResult.fail("无效的宠物类型");
        }

        UserPet pet = getOrCreatePet(userId);
        if (newType.equals(pet.getPetType())) {
            return TypeChangeResult.fail("当前已是该形象");
        }

        // 冷却检查（首次免费选择不检查冷却）
        if (!free) {
            String cooldownMsg = checkTypeChangeCooldown(pet);
            if (cooldownMsg != null) {
                return TypeChangeResult.fail(cooldownMsg);
            }
        }

        // 扣积分（首次免费不扣）
        if (!free) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return TypeChangeResult.fail("用户不存在");
            if (user.getPetPoints() < TYPE_CHANGE_COST) {
                return TypeChangeResult.fail("积分不足，需要 " + TYPE_CHANGE_COST + " 积分，当前 " + user.getPetPoints());
            }
            user.setPetPoints(user.getPetPoints() - TYPE_CHANGE_COST);
            userRepository.save(user);
        }

        pet.setPetType(newType);
        pet.setLastTypeChangeTime(LocalDateTime.now());
        petRepository.save(pet);

        if (free) {
            return TypeChangeResult.success("形象已设置为 " + pet.getPetTypeName() + "！", pet, 0);
        }
        return TypeChangeResult.success("形象已更换为 " + pet.getPetTypeName() + "！（-" + TYPE_CHANGE_COST + " 积分）", pet, TYPE_CHANGE_COST);
    }

    /** 首次免费选择形象（注册后选择） */
    @Transactional
    public TypeChangeResult selectPetType(Long userId, String newType) {
        return changePetType(userId, newType, true);
    }

    private String checkTypeChangeCooldown(UserPet pet) {
        if (pet.getLastTypeChangeTime() == null) return null;
        long days = ChronoUnit.DAYS.between(pet.getLastTypeChangeTime(), LocalDateTime.now());
        if (days < TYPE_CHANGE_COOLDOWN_DAYS) {
            long remain = TYPE_CHANGE_COOLDOWN_DAYS - days;
            return "形象更换冷却中，还需等待 " + remain + " 天";
        }
        return null;
    }

    // ==================== 背包查询 ====================

    public List<InventoryView> getInventory(Long userId) {
        List<UserPetInventory> invList = inventoryRepository.findByUserId(userId);
        List<InventoryView> views = new ArrayList<>();
        for (UserPetInventory inv : invList) {
            PetShopItem item = shopItemRepository.findById(inv.getItemId()).orElse(null);
            if (item != null) {
                views.add(new InventoryView(inv, item));
            }
        }
        return views;
    }

    // ==================== View Classes ====================

    public static class PetView {
        public final UserPet pet;
        public final int points;

        public PetView(UserPet pet, int points) {
            this.pet = pet;
            this.points = points;
        }
    }

    public static class InventoryView {
        public final UserPetInventory inventory;
        public final PetShopItem item;
        public final String itemName;
        public final String itemType;
        public final int price;

        public InventoryView(UserPetInventory inventory, PetShopItem item) {
            this.inventory = inventory;
            this.item = item;
            this.itemName = item.getName();
            this.itemType = item.getType().name();
            this.price = item.getPrice();
        }
    }

    public static class BuyResult {
        public final boolean success;
        public final String message;
        public final int remainingPoints;
        public final UserPetInventory inventory;

        private BuyResult(boolean success, String message, int points, UserPetInventory inv) {
            this.success = success;
            this.message = message;
            this.remainingPoints = points;
            this.inventory = inv;
        }

        public static BuyResult success(String msg, int pts, UserPetInventory inv) {
            return new BuyResult(true, msg, pts, inv);
        }
        public static BuyResult fail(String msg) {
            return new BuyResult(false, msg, 0, null);
        }
    }

    public static class UseResult {
        public final boolean success;
        public final String message;
        public final UserPet pet;

        private UseResult(boolean success, String message, UserPet pet) {
            this.success = success;
            this.message = message;
            this.pet = pet;
        }

        public static UseResult success(String msg, UserPet pet) {
            return new UseResult(true, msg, pet);
        }
        public static UseResult fail(String msg) {
            return new UseResult(false, msg, null);
        }
    }

    public static class RenameResult {
        public final boolean success;
        public final String message;
        public final UserPet pet;

        private RenameResult(boolean success, String message, UserPet pet) {
            this.success = success;
            this.message = message;
            this.pet = pet;
        }

        public static RenameResult success(String msg, UserPet pet) {
            return new RenameResult(true, msg, pet);
        }
        public static RenameResult fail(String msg, UserPet pet) {
            return new RenameResult(false, msg, pet);
        }
    }

    public static class TypeChangeResult {
        public final boolean success;
        public final String message;
        public final UserPet pet;
        public final int cost;

        private TypeChangeResult(boolean success, String message, UserPet pet, int cost) {
            this.success = success;
            this.message = message;
            this.pet = pet;
            this.cost = cost;
        }

        public static TypeChangeResult success(String msg, UserPet pet, int cost) {
            return new TypeChangeResult(true, msg, pet, cost);
        }
        public static TypeChangeResult fail(String msg) {
            return new TypeChangeResult(false, msg, null, 0);
        }
    }
}
