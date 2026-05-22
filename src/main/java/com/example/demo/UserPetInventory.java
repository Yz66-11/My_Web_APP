package com.example.demo;

import jakarta.persistence.*;

/**
 * 用户宠物道具背包。
 * 每个用户 + 每种道具只存一行，quantity 表示拥有数量。
 */
@Entity
@Table(name = "user_pet_inventories", indexes = {
    @Index(name = "idx_inv_user_item", columnList = "user_id, item_id", unique = true)
})
public class UserPetInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(nullable = false)
    private int quantity = 0;

    public UserPetInventory() {}

    public UserPetInventory(Long userId, Long itemId, int quantity) {
        this.userId = userId;
        this.itemId = itemId;
        this.quantity = quantity;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
