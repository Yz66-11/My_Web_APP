package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final PostService postService;
    private final UserService userService;
    private final ShopService shopService;

    public AdminController(PostService postService, UserService userService, ShopService shopService) {
        this.postService = postService;
        this.userService = userService;
        this.shopService = shopService;
    }

    // ==================== Dashboard ====================

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userService.countUsers());
        model.addAttribute("approvedShopCount", shopService.countApproved());
        model.addAttribute("pendingShopCount", shopService.countPending());
        model.addAttribute("pendingShops", shopService.getPendingShops());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pendingCount", postService.countPendingPosts());
        return "admin/dashboard";
    }

    // ==================== Shop Review ====================

    @PostMapping("/shops/{id}/approve")
    public String approveShop(@PathVariable Long id) {
        shopService.approveShop(id);
        return "redirect:/admin";
    }

    @PostMapping("/shops/{id}/reject")
    public String rejectShop(@PathVariable Long id) {
        shopService.rejectShop(id);
        return "redirect:/admin";
    }

    // ==================== User Management ====================

    @PostMapping("/users/{id}/toggle-role")
    public String toggleUserRole(@PathVariable Long id) {
        User user = userService.findById(id).orElse(null);
        if (user != null) {
            userService.changeRole(id, user.getRole() == User.Role.USER ? User.Role.ADMIN : User.Role.USER);
        }
        return "redirect:/admin";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin";
    }

    // ==================== Post Review ====================

    @GetMapping("/posts")
    public String adminPosts(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        String currentStatus = (status != null) ? status : "pending";
        model.addAttribute("currentStatus", currentStatus);

        if ("pending".equals(currentStatus)) {
            Page<Post> result = postService.getPendingPosts(page, 10);
            model.addAttribute("posts", result.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasNext", !result.isLast());
            model.addAttribute("hasPrev", !result.isFirst());
        } else {
            Page<Post> result = postService.getAllPostsWithStatus(page, 10);
            model.addAttribute("posts", result.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasNext", !result.isLast());
            model.addAttribute("hasPrev", !result.isFirst());
        }

        model.addAttribute("pendingCount", postService.countPendingPosts());
        return "admin/posts";
    }

    @PostMapping("/post/{id}/approve")
    public String approvePost(@PathVariable Long id,
                              @RequestParam(value = "page", defaultValue = "0") int page,
                              @RequestParam(value = "status", defaultValue = "pending") String status) {
        postService.approvePost(id);
        return "redirect:/admin/posts?status=" + status + "&page=" + page;
    }

    @PostMapping("/post/{id}/reject")
    public String rejectPost(@PathVariable Long id,
                             @RequestParam(value = "page", defaultValue = "0") int page,
                             @RequestParam(value = "status", defaultValue = "pending") String status) {
        postService.rejectPost(id);
        return "redirect:/admin/posts?status=" + status + "&page=" + page;
    }

    @PostMapping("/post/{id}/delete")
    public String adminDeletePost(@PathVariable Long id,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  @RequestParam(value = "status", defaultValue = "pending") String status) {
        postService.adminDeletePost(id);
        return "redirect:/admin/posts?status=" + status + "&page=" + page;
    }

    // ==================== Comment Management ====================

    @GetMapping("/comments")
    public String adminComments(
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        List<Comment> comments = postService.getAllComments(page, 20);
        model.addAttribute("comments", comments);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalComments", postService.countAllComments());
        model.addAttribute("pendingCount", postService.countPendingPosts());
        model.addAttribute("hasMore", comments.size() == 20);
        return "admin/comments";
    }

    @PostMapping("/comment/{id}/delete")
    public String adminDeleteComment(@PathVariable Long id,
                                     @RequestParam(value = "page", defaultValue = "0") int page) {
        postService.adminDeleteComment(id);
        return "redirect:/admin/comments?page=" + page;
    }
}
