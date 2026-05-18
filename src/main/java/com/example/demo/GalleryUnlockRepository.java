package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GalleryUnlockRepository extends JpaRepository<GalleryUnlock, Long> {

    Optional<GalleryUnlock> findByUserIdAndDishId(Long userId, Long dishId);

    List<GalleryUnlock> findByUserId(Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndDishId(Long userId, Long dishId);

    @Query("SELECT gu.dish.id FROM GalleryUnlock gu WHERE gu.user.id = :userId")
    List<Long> findUnlockedDishIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT gu.dish.shop.id FROM GalleryUnlock gu WHERE gu.user.id = :userId")
    List<Long> findUnlockedShopIdsByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
