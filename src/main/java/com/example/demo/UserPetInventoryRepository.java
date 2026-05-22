package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserPetInventoryRepository extends JpaRepository<UserPetInventory, Long> {

    List<UserPetInventory> findByUserId(Long userId);

    Optional<UserPetInventory> findByUserIdAndItemId(Long userId, Long itemId);

    void deleteByUserId(Long userId);
}
