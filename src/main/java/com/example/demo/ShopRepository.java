package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long> {

    List<Shop> findByStatus(Shop.ShopStatus status);

    List<Shop> findByOwnerId(Long ownerId);

    List<Shop> findByStatusOrderByCreatedAtDesc(Shop.ShopStatus status);

    @Query("SELECT s FROM Shop s WHERE s.status = :status ORDER BY s.updatedAt DESC")
    List<Shop> findByStatusOrderByUpdatedAtDesc(@Param("status") Shop.ShopStatus status);

    List<Shop> findByCategory(String category);

    long countByStatus(Shop.ShopStatus status);

    List<Shop> findByStatusAndCity(Shop.ShopStatus status, String city);

    List<Shop> findByStatusAndCityAndDistrict(Shop.ShopStatus status, String city, String district);

    @Query("SELECT DISTINCT s.city FROM Shop s WHERE s.status IN ('APPROVED','CLOSED') AND s.city IS NOT NULL ORDER BY s.city")
    List<String> findDistinctCities();

    @Query("SELECT DISTINCT s.district FROM Shop s WHERE s.status IN ('APPROVED','CLOSED') AND s.city = :city AND s.district IS NOT NULL ORDER BY s.district")
    List<String> findDistinctDistrictsByCity(@Param("city") String city);

    @Query("SELECT s FROM Shop s WHERE s.status IN ('APPROVED','CLOSED') AND s.city = :city")
    List<Shop> findByActiveStatusAndCity(@Param("city") String city);

    @Query("SELECT s FROM Shop s WHERE s.status IN ('APPROVED','CLOSED') AND s.city = :city AND s.district = :district")
    List<Shop> findByActiveStatusAndCityAndDistrict(@Param("city") String city, @Param("district") String district);
}
