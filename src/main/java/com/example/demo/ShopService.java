package com.example.demo;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ShopService {

    private final ShopRepository shopRepository;
    private final DishRepository dishRepository;

    public ShopService(ShopRepository shopRepository, DishRepository dishRepository) {
        this.shopRepository = shopRepository;
        this.dishRepository = dishRepository;
    }

    public List<Shop> getAllShops() {
        return shopRepository.findByStatus(Shop.ShopStatus.APPROVED);
    }

    public List<Shop> getPendingShops() {
        return shopRepository.findByStatusOrderByCreatedAtDesc(Shop.ShopStatus.PENDING);
    }

    public Optional<Shop> findById(Long id) {
        return shopRepository.findById(id);
    }

    public List<Shop> getShopsByOwner(Long ownerId) {
        return shopRepository.findByOwnerId(ownerId);
    }

    public List<Dish> getDishesByShopId(Long shopId) {
        return dishRepository.findByShopId(shopId);
    }

    public long countPending() {
        return shopRepository.countByStatus(Shop.ShopStatus.PENDING);
    }

    public long countApproved() {
        return shopRepository.countByStatus(Shop.ShopStatus.APPROVED);
    }

    public Shop applyShop(Shop shop, User owner) {
        shop.setOwner(owner);
        shop.setStatus(Shop.ShopStatus.PENDING);
        return shopRepository.save(shop);
    }

    public void approveShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("店铺不存在"));
        shop.setStatus(Shop.ShopStatus.APPROVED);
        shopRepository.save(shop);
    }

    public void rejectShop(Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("店铺不存在"));
        shop.setStatus(Shop.ShopStatus.REJECTED);
        shopRepository.save(shop);
    }

    public Dish addDish(Dish dish, Long shopId) {
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("店铺不存在"));
        dish.setShop(shop);
        return dishRepository.save(dish);
    }

    public void deleteDish(Long dishId) {
        dishRepository.deleteById(dishId);
    }

    public List<Shop> searchShops(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllShops();
        }
        String kw = keyword.trim();
        return getAllShops().stream()
                .filter(s -> s.getShopName().contains(kw) ||
                        (s.getCategory() != null && s.getCategory().contains(kw)) ||
                        (s.getLocation() != null && s.getLocation().contains(kw)))
                .toList();
    }
}
