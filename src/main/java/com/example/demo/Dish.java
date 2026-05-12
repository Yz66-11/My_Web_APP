package com.example.demo;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dishes")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String dishName;

    private BigDecimal price;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    /** AVAILABLE=上架, UNAVAILABLE=下架 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DishStatus status = DishStatus.AVAILABLE;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "shop_id")
    private Shop shop;

    public enum DishStatus {
        AVAILABLE("上架"),
        UNAVAILABLE("下架");

        private final String label;
        DishStatus(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public Dish() {
        this.createdAt = LocalDateTime.now();
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDishName() { return dishName; }
    public void setDishName(String dishName) { this.dishName = dishName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public DishStatus getStatus() { return status; }
    public void setStatus(DishStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Shop getShop() { return shop; }
    public void setShop(Shop shop) { this.shop = shop; }
}
