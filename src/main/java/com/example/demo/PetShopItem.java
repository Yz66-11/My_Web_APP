package com.example.demo;

import jakarta.persistence.*;

/**
 * 宠物商店道具表。
 * 类型枚举：FOOD(食物，加经验), DECORATION(装扮，改外观), SPECIAL(特殊道具)
 */
@Entity
@Table(name = "pet_shop_items")
public class PetShopItem {

    public enum ItemType {
        FOOD, DECORATION, SPECIAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    /** 价格（积分） */
    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType type;

    /** 效果值：FOOD为经验值，DECORATION为装扮部位key（如hat/outfit），SPECIAL为特殊效果 */
    @Column(name = "effect_key", length = 30)
    private String effectKey;

    /** 效果数值（如经验+50） */
    @Column(name = "effect_value")
    private int effectValue;

    @Column(length = 20)
    private String iconUrl;

    @Column(length = 200)
    private String description;

    /** 是否在售 */
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int sortOrder = 0;

    public PetShopItem() {}

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getPrice() { return price; }
    public void setPrice(int price) { this.price = price; }

    public ItemType getType() { return type; }
    public void setType(ItemType type) { this.type = type; }

    public String getEffectKey() { return effectKey; }
    public void setEffectKey(String effectKey) { this.effectKey = effectKey; }

    public int getEffectValue() { return effectValue; }
    public void setEffectValue(int effectValue) { this.effectValue = effectValue; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
