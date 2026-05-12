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

    public AdminController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    private Long getUserId(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName()).map(User::getId).orElse(null);
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
