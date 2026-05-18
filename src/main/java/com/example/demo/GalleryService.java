package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GalleryService {

    private final ShopRepository shopRepository;
    private final DishRepository dishRepository;
    private final GalleryUnlockRepository unlockRepository;
    private final UserRepository userRepository;
    private final ShopService shopService;

    public GalleryService(ShopRepository shopRepository, DishRepository dishRepository,
                          GalleryUnlockRepository unlockRepository, UserRepository userRepository,
                          ShopService shopService) {
        this.shopRepository = shopRepository;
        this.dishRepository = dishRepository;
        this.unlockRepository = unlockRepository;
        this.userRepository = userRepository;
        this.shopService = shopService;
    }

    // ==================== City Level ====================

    public List<CityView> getCities(Long userId) {
        List<String> cityNames = shopRepository.findDistinctCities();
        Set<Long> unlockedDishIds = new HashSet<>(unlockRepository.findUnlockedDishIdsByUserId(userId));
        Set<Long> unlockedShopIds = new HashSet<>(unlockRepository.findUnlockedShopIdsByUserId(userId));

        List<CityView> cities = new ArrayList<>();
        for (String cityName : cityNames) {
            List<Shop> shops = shopService.getActiveShopsByCity(cityName);
            if (shops.isEmpty()) continue;

            Set<String> districts = new LinkedHashSet<>();
            int totalDishes = 0;
            int unlockedDishes = 0;
            int unlockedShops = 0;

            for (Shop shop : shops) {
                if (shop.getDistrict() != null) districts.add(shop.getDistrict());
                if (shop.getStatus() == Shop.ShopStatus.CLOSED) continue; // 闭店不计入可解锁统计
                List<Dish> dishes = dishRepository.findByShopIdAndStatus(shop.getId(), Dish.DishStatus.AVAILABLE);
                totalDishes += dishes.size();
                boolean shopHasUnlock = false;
                for (Dish dish : dishes) {
                    if (unlockedDishIds.contains(dish.getId())) {
                        unlockedDishes++;
                        shopHasUnlock = true;
                    }
                }
                if (shopHasUnlock) unlockedShops++;
            }

            cities.add(new CityView(cityName, districts.size(), shops.size(),
                    totalDishes, unlockedShops, unlockedDishes));
        }

        if (userId != null) {
            cities.sort((a, b) -> Double.compare(b.getProgress(), a.getProgress()));
        }
        return cities;
    }

    // ==================== District Level ====================

    public DistrictPageView getDistricts(Long userId, String city) {
        List<String> districtNames = shopRepository.findDistinctDistrictsByCity(city);
        Set<Long> unlockedDishIds = new HashSet<>(unlockRepository.findUnlockedDishIdsByUserId(userId));

        List<DistrictView> districts = new ArrayList<>();
        for (String districtName : districtNames) {
            List<Shop> shops = shopService.getActiveShopsByCityAndDistrict(city, districtName);
            int totalDishes = 0;
            int unlockedDishes = 0;
            int unlockedShops = 0;

            for (Shop shop : shops) {
                if (shop.getStatus() == Shop.ShopStatus.CLOSED) continue;
                List<Dish> dishes = dishRepository.findByShopIdAndStatus(shop.getId(), Dish.DishStatus.AVAILABLE);
                totalDishes += dishes.size();
                boolean shopHasUnlock = false;
                for (Dish dish : dishes) {
                    if (unlockedDishIds.contains(dish.getId())) {
                        unlockedDishes++;
                        shopHasUnlock = true;
                    }
                }
                if (shopHasUnlock) unlockedShops++;
            }

            districts.add(new DistrictView(districtName, shops.size(),
                    totalDishes, unlockedShops, unlockedDishes));
        }

        return new DistrictPageView(city, districts);
    }

    // ==================== Shop Level ====================

    public ShopPageView getShops(Long userId, String city, String district) {
        List<Shop> shops = shopService.getActiveShopsByCityAndDistrict(city, district);
        Set<Long> unlockedDishIds = new HashSet<>(unlockRepository.findUnlockedDishIdsByUserId(userId));

        List<ShopView> shopViews = new ArrayList<>();
        for (Shop shop : shops) {
            List<Dish> dishes = dishRepository.findByShopIdAndStatus(shop.getId(), Dish.DishStatus.AVAILABLE);
            int unlockedCount = 0;
            for (Dish dish : dishes) {
                if (unlockedDishIds.contains(dish.getId())) unlockedCount++;
            }
            boolean closed = shop.getStatus() == Shop.ShopStatus.CLOSED;
            shopViews.add(new ShopView(shop, dishes.size(), unlockedCount, closed));
        }

        return new ShopPageView(city, district, shopViews);
    }

    // ==================== Dish Level ====================

    public DishPageView getDishes(Long userId, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("店铺不存在"));
        List<Dish> dishes = dishRepository.findByShopId(shopId);
        Set<Long> unlockedDishIds = new HashSet<>(unlockRepository.findUnlockedDishIdsByUserId(userId));
        Map<Long, String> unlockImageMap = buildUnlockImageMap(userId);

        List<DishView> dishViews = new ArrayList<>();
        for (Dish dish : dishes) {
            boolean unlocked = unlockedDishIds.contains(dish.getId());
            String imageUrl = getDisplayImage(unlocked, dish, unlockImageMap);
            boolean unavailable = dish.getStatus() == Dish.DishStatus.UNAVAILABLE;
            dishViews.add(new DishView(dish, unlocked, unavailable, imageUrl));
        }

        return new DishPageView(shop, dishViews);
    }

    // ==================== Search ====================

    public List<SearchResultView> search(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Collections.emptyList();
        String kw = keyword.trim().toLowerCase();
        Set<Long> unlockedDishIds = new HashSet<>(unlockRepository.findUnlockedDishIdsByUserId(userId));
        Map<Long, String> unlockImageMap = buildUnlockImageMap(userId);

        List<Shop> allShops = new ArrayList<>(shopRepository.findByStatus(Shop.ShopStatus.APPROVED));
        allShops.addAll(shopRepository.findByStatus(Shop.ShopStatus.CLOSED));
        List<SearchResultView> results = new ArrayList<>();

        for (Shop shop : allShops) {
            boolean shopMatch = (shop.getShopName() != null && shop.getShopName().toLowerCase().contains(kw))
                    || (shop.getCategory() != null && shop.getCategory().toLowerCase().contains(kw))
                    || (shop.getCity() != null && shop.getCity().toLowerCase().contains(kw))
                    || (shop.getDistrict() != null && shop.getDistrict().toLowerCase().contains(kw));

            List<Dish> dishes = dishRepository.findByShopId(shop.getId());
            List<DishView> matchedDishes = new ArrayList<>();

            for (Dish dish : dishes) {
                boolean dishMatch = (dish.getDishName() != null && dish.getDishName().toLowerCase().contains(kw))
                        || (dish.getDescription() != null && dish.getDescription().toLowerCase().contains(kw));
                if (shopMatch || dishMatch) {
                    boolean unlocked = unlockedDishIds.contains(dish.getId());
                    String imageUrl = getDisplayImage(unlocked, dish, unlockImageMap);
                    boolean unavailable = dish.getStatus() == Dish.DishStatus.UNAVAILABLE;
                    matchedDishes.add(new DishView(dish, unlocked, unavailable, imageUrl));
                }
            }

            if (!matchedDishes.isEmpty()) {
                results.add(new SearchResultView(shop, matchedDishes));
            }
        }
        return results;
    }

    // ==================== Unlock ====================

    @Transactional
    public boolean unlockDish(Long userId, Long dishId, String imageUrl) {
        if (unlockRepository.existsByUserIdAndDishId(userId, dishId)) {
            return false;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new RuntimeException("菜品不存在"));
        unlockRepository.save(new GalleryUnlock(user, dish, imageUrl));
        return true;
    }

    // ==================== Global Stats ====================

    public GlobalStats getGlobalStats(Long userId) {
        long totalDishes = dishRepository.count();
        long unlockedDishes = unlockRepository.countByUserId(userId);
        return new GlobalStats(totalDishes, unlockedDishes);
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建 dishId -> 打卡照片URL 的映射表
     */
    private Map<Long, String> buildUnlockImageMap(Long userId) {
        Map<Long, String> map = new HashMap<>();
        if (userId == null) return map;
        List<GalleryUnlock> unlocks = unlockRepository.findByUserId(userId);
        for (GalleryUnlock unlock : unlocks) {
            if (unlock.getImageUrl() != null) {
                map.put(unlock.getDish().getId(), unlock.getImageUrl());
            }
        }
        return map;
    }

    /**
     * 获取菜品显示图片：已解锁优先展示打卡照片，否则展示菜品默认图，都没有则返回 null
     */
    private String getDisplayImage(boolean unlocked, Dish dish, Map<Long, String> unlockImageMap) {
        if (unlocked) {
            String checkinImage = unlockImageMap.get(dish.getId());
            if (checkinImage != null) return checkinImage;
        }
        return dish.getImageUrl();
    }

    // ==================== Inner View Classes ====================

    public static class CityView {
        private final String name;
        private final int districtCount;
        private final int shopCount;
        private final int totalDishes;
        private final int unlockedShops;
        private final int unlockedDishes;

        public CityView(String name, int districtCount, int shopCount,
                        int totalDishes, int unlockedShops, int unlockedDishes) {
            this.name = name;
            this.districtCount = districtCount;
            this.shopCount = shopCount;
            this.totalDishes = totalDishes;
            this.unlockedShops = unlockedShops;
            this.unlockedDishes = unlockedDishes;
        }

        public String getName() { return name; }
        public int getDistrictCount() { return districtCount; }
        public int getShopCount() { return shopCount; }
        public int getTotalDishes() { return totalDishes; }
        public int getUnlockedShops() { return unlockedShops; }
        public int getUnlockedDishes() { return unlockedDishes; }
        public double getProgress() { return totalDishes == 0 ? 0 : (double) unlockedDishes / totalDishes * 100; }
    }

    public static class DistrictView {
        private final String name;
        private final int shopCount;
        private final int totalDishes;
        private final int unlockedShops;
        private final int unlockedDishes;

        public DistrictView(String name, int shopCount, int totalDishes,
                            int unlockedShops, int unlockedDishes) {
            this.name = name;
            this.shopCount = shopCount;
            this.totalDishes = totalDishes;
            this.unlockedShops = unlockedShops;
            this.unlockedDishes = unlockedDishes;
        }

        public String getName() { return name; }
        public int getShopCount() { return shopCount; }
        public int getTotalDishes() { return totalDishes; }
        public int getUnlockedShops() { return unlockedShops; }
        public int getUnlockedDishes() { return unlockedDishes; }
        public double getProgress() { return totalDishes == 0 ? 0 : (double) unlockedDishes / totalDishes * 100; }
    }

    public static class ShopView {
        private final Shop shop;
        private final int totalDishes;
        private final int unlockedDishes;
        private final boolean closed;

        public ShopView(Shop shop, int totalDishes, int unlockedDishes) {
            this(shop, totalDishes, unlockedDishes, shop.getStatus() == Shop.ShopStatus.CLOSED);
        }

        public ShopView(Shop shop, int totalDishes, int unlockedDishes, boolean closed) {
            this.shop = shop;
            this.totalDishes = totalDishes;
            this.unlockedDishes = unlockedDishes;
            this.closed = closed;
        }

        public Shop getShop() { return shop; }
        public int getTotalDishes() { return totalDishes; }
        public int getUnlockedDishes() { return unlockedDishes; }
        public boolean isClosed() { return closed; }
        public double getProgress() { return totalDishes == 0 ? 0 : (double) unlockedDishes / totalDishes * 100; }
    }

    public static class DishView {
        private final Dish dish;
        private final boolean unlocked;
        private final boolean unavailable;
        private final String imageUrl;

        public DishView(Dish dish, boolean unlocked) {
            this(dish, unlocked, false, null);
        }

        public DishView(Dish dish, boolean unlocked, String imageUrl) {
            this(dish, unlocked, false, imageUrl);
        }

        public DishView(Dish dish, boolean unlocked, boolean unavailable, String imageUrl) {
            this.dish = dish;
            this.unlocked = unlocked;
            this.unavailable = unavailable;
            this.imageUrl = imageUrl;
        }

        public Dish getDish() { return dish; }
        public boolean isUnlocked() { return unlocked; }
        public boolean isUnavailable() { return unavailable; }
        /** 打卡照片 URL（已解锁时优先展示），若为空则使用 dish.getImageUrl() 或默认图标 */
        public String getImageUrl() { return imageUrl; }
    }

    public static class SearchResultView {
        private final Shop shop;
        private final List<DishView> dishes;

        public SearchResultView(Shop shop, List<DishView> dishes) {
            this.shop = shop;
            this.dishes = dishes;
        }

        public Shop getShop() { return shop; }
        public List<DishView> getDishes() { return dishes; }
    }

    public static class DistrictPageView {
        private final String city;
        private final List<DistrictView> districts;

        public DistrictPageView(String city, List<DistrictView> districts) {
            this.city = city;
            this.districts = districts;
        }

        public String getCity() { return city; }
        public List<DistrictView> getDistricts() { return districts; }
    }

    public static class ShopPageView {
        private final String city;
        private final String district;
        private final List<ShopView> shops;

        public ShopPageView(String city, String district, List<ShopView> shops) {
            this.city = city;
            this.district = district;
            this.shops = shops;
        }

        public String getCity() { return city; }
        public String getDistrict() { return district; }
        public List<ShopView> getShops() { return shops; }
    }

    public static class DishPageView {
        private final Shop shop;
        private final List<DishView> dishes;

        public DishPageView(Shop shop, List<DishView> dishes) {
            this.shop = shop;
            this.dishes = dishes;
        }

        public Shop getShop() { return shop; }
        public List<DishView> getDishes() { return dishes; }
    }

    public static class GlobalStats {
        private final long totalDishes;
        private final long unlockedDishes;

        public GlobalStats(long totalDishes, long unlockedDishes) {
            this.totalDishes = totalDishes;
            this.unlockedDishes = unlockedDishes;
        }

        public long getTotalDishes() { return totalDishes; }
        public long getUnlockedDishes() { return unlockedDishes; }
        public double getProgress() { return totalDishes == 0 ? 0 : (double) unlockedDishes / totalDishes * 100; }
    }
}
