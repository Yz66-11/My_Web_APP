package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    List<Dish> findByShopId(Long shopId);

    List<Dish> findByShopIdAndStatus(Long shopId, Dish.DishStatus status);

    long countByShopId(Long shopId);

    Optional<Dish> findByShopIdAndId(Long shopId, Long id);
}
