package com.example.demo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ShopRepository shopRepository;
    private final DishRepository dishRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostFavoriteRepository postFavoriteRepository;
    private final CommentRepository commentRepository;
    private final GalleryUnlockRepository galleryUnlockRepository;
    private final UserPetRepository userPetRepository;
    private final UserPetInventoryRepository userPetInventoryRepository;
    private final PetService petService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       ShopRepository shopRepository, DishRepository dishRepository,
                       PostRepository postRepository,
                       PostLikeRepository postLikeRepository,
                       PostFavoriteRepository postFavoriteRepository,
                       CommentRepository commentRepository,
                       GalleryUnlockRepository galleryUnlockRepository,
                       UserPetRepository userPetRepository,
                       UserPetInventoryRepository userPetInventoryRepository,
                       PetService petService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.shopRepository = shopRepository;
        this.dishRepository = dishRepository;
        this.postRepository = postRepository;
        this.postLikeRepository = postLikeRepository;
        this.postFavoriteRepository = postFavoriteRepository;
        this.commentRepository = commentRepository;
        this.galleryUnlockRepository = galleryUnlockRepository;
        this.userPetRepository = userPetRepository;
        this.userPetInventoryRepository = userPetInventoryRepository;
        this.petService = petService;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public long countUsers() {
        return userRepository.count();
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.USER);
        User saved = userRepository.save(user);
        // 自动授予宠物
        try {
            petService.grantPet(saved.getId());
        } catch (Exception ignored) {
            // 宠物授予失败不影响注册流程
        }
        return saved;
    }

    public User createAdminUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(User.Role.ADMIN);
        return userRepository.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void resetPassword(String username, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void updateProfile(String username, String name, String phone, Integer gender, Integer age, String bio) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setName(name);
        user.setPhone(phone);
        user.setGender(gender);
        user.setAge(age);
        user.setBio(bio);
        userRepository.save(user);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public void changeRole(Long userId, User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        // 0. 删除宠物相关数据
        userPetInventoryRepository.deleteByUserId(userId);
        userPetRepository.findByUserId(userId).ifPresent(userPetRepository::delete);

        // 1. 删除图鉴解锁记录
        galleryUnlockRepository.deleteByUserId(userId);

        // 2. 删除该用户的评论
        commentRepository.deleteByAuthorId(userId);

        // 3. 删除该用户的点赞
        postLikeRepository.deleteByUserId(userId);

        // 4. 删除该用户的收藏
        postFavoriteRepository.deleteByUserId(userId);

        // 5. 删除该用户的帖子（先清理帖子关联的评论、点赞、收藏）
        List<Post> userPosts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId);
        for (Post post : userPosts) {
            postLikeRepository.deleteAllByPostId(post.getId());
            postFavoriteRepository.deleteAllByPostId(post.getId());
            commentRepository.deleteAllByPostId(post.getId());
            postRepository.delete(post);
        }

        // 6. 删除该用户的店铺（先删菜品，再删店铺）
        List<Shop> userShops = shopRepository.findByOwnerId(userId);
        for (Shop shop : userShops) {
            dishRepository.deleteAll(dishRepository.findByShopId(shop.getId()));
            shopRepository.delete(shop);
        }

        // 7. 最后删除用户
        userRepository.deleteById(userId);
    }

    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        user.setRole(user.getRole() == User.Role.USER ? User.Role.ADMIN : User.Role.USER);
        userRepository.save(user);
    }

    // ==================== 统计 ====================

    public long countUserCheckins(Long userId) {
        return galleryUnlockRepository.countByUserId(userId);
    }

    public long countUserFavorites(Long userId) {
        return postFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).size();
    }
}
