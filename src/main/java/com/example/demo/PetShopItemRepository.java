package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PetShopItemRepository extends JpaRepository<PetShopItem, Long> {

    List<PetShopItem> findByActiveTrueOrderBySortOrderAsc();

    List<PetShopItem> findByTypeAndActiveTrueOrderBySortOrderAsc(PetShopItem.ItemType type);
}
