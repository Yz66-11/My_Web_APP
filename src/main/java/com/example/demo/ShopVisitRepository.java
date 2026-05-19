package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ShopVisitRepository extends JpaRepository<ShopVisit, Long> {
    Optional<ShopVisit> findByUserIdAndShopId(Long userId, Long shopId);
    boolean existsByUserIdAndShopId(Long userId, Long shopId);
    java.util.List<ShopVisit> findByUserId(Long userId);
    long countByShopId(Long shopId);

    @Query("SELECT COUNT(sv) FROM ShopVisit sv WHERE sv.userId = :userId")
    long countByUserId(@Param("userId") Long userId);
}
