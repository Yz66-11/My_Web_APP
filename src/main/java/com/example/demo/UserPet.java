package com.example.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_pets", indexes = {
    @Index(name = "idx_user_pet_user_id", columnList = "user_id", unique = true)
})
public class UserPet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID（一对一关系，每个用户一只宠物） */
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** 宠物类型标识: panda / cat / dog / lion / tiger */
    @Column(nullable = false, length = 20)
    private String petType = "panda";

    /** 宠物昵称 */
    @Column(length = 30)
    private String nickname;

    /** 等级 1-50 */
    @Column(nullable = false)
    private int level = 1;

    /** 当前经验值 */
    @Column(name = "experience", nullable = false)
    private int experience = 0;

    /** 升级所需经验 = level * 100 */
    @Column(name = "exp_to_next", nullable = false)
    private int expToNextLevel = 100;

    /** 上次改名时间（冷却15天） */
    @Column(name = "last_rename_time")
    private LocalDateTime lastRenameTime;

    /** 上次更换形象时间（冷却30天） */
    @Column(name = "last_type_change_time")
    private LocalDateTime lastTypeChangeTime;

    /**
     * 外观JSON，存储装扮状态：
     * {"body":"default","hat":"none","outfit":"none","accessory":"none","effect":"none"}
     */
    @Column(name = "appearance_json", columnDefinition = "TEXT")
    private String appearanceJson = "{\"body\":\"default\",\"hat\":\"none\",\"outfit\":\"none\",\"accessory\":\"none\",\"effect\":\"none\"}";

    public UserPet() {}

    public UserPet(Long userId, String petType, String nickname) {
        this.userId = userId;
        this.petType = petType != null ? petType : "panda";
        this.nickname = nickname != null ? nickname : "小可爱";
        this.level = 1;
        this.experience = 0;
        this.expToNextLevel = 100;
    }

    // ===== Getters & Setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPetType() { return petType; }
    public void setPetType(String petType) { this.petType = petType; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getExpToNextLevel() { return expToNextLevel; }
    public void setExpToNextLevel(int expToNextLevel) { this.expToNextLevel = expToNextLevel; }

    public String getAppearanceJson() { return appearanceJson; }
    public void setAppearanceJson(String appearanceJson) { this.appearanceJson = appearanceJson; }

    public LocalDateTime getLastRenameTime() { return lastRenameTime; }
    public void setLastRenameTime(LocalDateTime lastRenameTime) { this.lastRenameTime = lastRenameTime; }

    public LocalDateTime getLastTypeChangeTime() { return lastTypeChangeTime; }
    public void setLastTypeChangeTime(LocalDateTime lastTypeChangeTime) { this.lastTypeChangeTime = lastTypeChangeTime; }

    /** 获取宠物emoji（根据类型） */
    public String getPetEmoji() {
        if (petType == null) return "\uD83D\uDC3C"; // 🐼
        switch (petType) {
            case "panda": return "\uD83D\uDC3C"; // 🐼
            case "cat":   return "\uD83D\uDC31"; // 🐱
            case "dog":   return "\uD83D\uDC36"; // 🐶
            case "lion":  return "\uD83E\uDD81"; // 🦁
            case "tiger": return "\uD83D\uDC2F"; // 🐯
            default: return "\uD83D\uDC3C";
        }
    }

    /** 获取类型中文名 */
    public String getPetTypeName() {
        if (petType == null) return "熊猫";
        switch (petType) {
            case "panda": return "熊猫";
            case "cat":   return "猫咪";
            case "dog":   return "小狗";
            case "lion":  return "狮子";
            case "tiger": return "老虎";
            default: return petType;
        }
    }
}
