package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.FileOutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Controller
public class PostController {

    private final PostService postService;
    private final UserService userService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public PostController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    private Long getUserId(Principal principal) {
        if (principal == null) return null;
        return userService.findByUsername(principal.getName()).map(User::getId).orElse(null);
    }

    // ==================== Request DTO ====================

    public static class PostCreateRequest {
        private String title;
        private String content;
        private String location;
        private List<String> images; // base64 图片列表

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public List<String> getImages() { return images; }
        public void setImages(List<String> images) { this.images = images; }
    }

    // ==================== Post List & Detail ====================

    @GetMapping("/posts")
    @Transactional(readOnly = true)
    public String showPosts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tab", required = false) String tab,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model, Principal principal) {
        Long userId = getUserId(principal);
        model.addAttribute("keyword", keyword);

        if ("favorites".equals(tab) && userId != null) {
            model.addAttribute("posts", postService.getMyFavorites(userId));
            model.addAttribute("isFavorites", true);
            model.addAttribute("currentTab", "favorites");
        } else if ("my".equals(tab) && userId != null) {
            model.addAttribute("posts", postService.getPostsByUser(userId, userId));
            model.addAttribute("isMyPosts", true);
            model.addAttribute("currentTab", "my");
        } else {
            Page<PostService.PostView> result = (keyword != null && !keyword.isBlank())
                    ? postService.searchPosts(keyword, page, 10, userId)
                    : postService.getAllPosts(page, 10, userId);
            model.addAttribute("posts", result.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", result.getTotalPages());
            model.addAttribute("hasNext", !result.isLast());
            model.addAttribute("hasPrev", !result.isFirst());
            model.addAttribute("currentTab", "latest");
        }
        return "posts";
    }

    @GetMapping("/post/{id}")
    public String showPostDetail(@PathVariable Long id, Model model, Principal principal) {
        Optional<Post> postOpt = postService.findById(id);
        if (postOpt.isEmpty()) return "redirect:/posts";

        Long userId = getUserId(principal);
        Post post = postOpt.get();
        boolean liked = userId != null
                && postService.isPostLiked(id, userId);
        boolean favorited = userId != null
                && postService.isPostFavorited(id, userId);
        boolean isAdmin = userId != null
                && userService.findByUsername(principal.getName()).map(u -> u.getRole() == User.Role.ADMIN).orElse(false);

        model.addAttribute("post", post);
        model.addAttribute("liked", liked);
        model.addAttribute("favorited", favorited);
        model.addAttribute("comments", postService.getComments(id));
        model.addAttribute("currentUserId", userId);
        model.addAttribute("isAdmin", isAdmin);
        return "post-detail";
    }

    @GetMapping("/post/create")
    public String showCreateForm(Model model) {
        return "post-create";
    }

    @PostMapping("/post/create")
    public String createPost(@RequestParam("title") String title,
                             @RequestParam("content") String content,
                             @RequestParam(value = "imageUrls", required = false) String imageUrls,
                             @RequestParam(value = "location", required = false) String location,
                             Principal principal, RedirectAttributes redirectAttributes) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        postService.createPost(userId, title, content, imageUrls, location);
        redirectAttributes.addFlashAttribute("message", "帖子提交成功，请等待管理员审核！");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/posts";
    }

    // ==================== JSON API: Create Post with Images & Location ====================

    @PostMapping("/api/post/create")
    @ResponseBody
    public String createPostApi(@RequestBody PostCreateRequest req, Principal principal) {
        try {
            Long userId = getUserId(principal);
            if (userId == null) {
                return "{\"success\": false, \"message\": \"未登录\"}";
            }

            // 保存上传的图片
            String imageUrls = null;
            if (req.getImages() != null && !req.getImages().isEmpty()) {
                List<String> urls = new ArrayList<>();
                for (String base64Data : req.getImages()) {
                    String url = savePostImage(base64Data, userId);
                    if (url != null) urls.add(url);
                }
                if (!urls.isEmpty()) {
                    imageUrls = String.join(",", urls);
                }
            }

            // 创建帖子
            postService.createPost(userId, req.getTitle(), req.getContent(), imageUrls, req.getLocation());
            return "{\"success\": true, \"message\": \"帖子提交成功，请等待管理员审核！\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"" + e.getMessage() + "\"}";
        }
    }

    // ==================== Image Save Helper ====================

    private String savePostImage(String base64Data, Long userId) throws Exception {
        // 移除 data:image/xxx;base64, 前缀
        String data = base64Data;
        if (data.contains(",")) {
            data = data.substring(data.indexOf(",") + 1);
        }
        byte[] imageBytes = Base64.getDecoder().decode(data);

        String dirPath = uploadDir + "/posts/";
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        String filename = userId + "_" + System.currentTimeMillis() + ".jpg";
        File file = new File(dirPath + filename);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageBytes);
        }

        return "/uploads/posts/" + filename;
    }

    @PostMapping("/post/{id}/like")
    public String toggleLike(@PathVariable Long id, Principal principal,
                             @RequestParam(value = "from", required = false) String from) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        postService.toggleLike(id, userId);
        return "redirect:" + (from != null ? from : "/post/" + id);
    }

    @PostMapping("/post/{id}/favorite")
    public String toggleFavorite(@PathVariable Long id, Principal principal,
                                  @RequestParam(value = "from", required = false) String from) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        postService.toggleFavorite(id, userId);
        return "redirect:" + (from != null ? from : "/post/" + id);
    }

    @PostMapping("/post/{id}/comment")
    public String addComment(@PathVariable Long id,
                             @RequestParam("content") String content,
                             Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        postService.addComment(id, userId, content);
        return "redirect:/post/" + id;
    }

    @PostMapping("/post/{id}/delete")
    public String deletePost(@PathVariable Long id, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        postService.deletePost(id, userId);
        return "redirect:/posts";
    }

    @PostMapping("/post/{pid}/comment/delete")
    public String deleteComment(@PathVariable("pid") Long postId, @RequestParam("id") Long commentId, Principal principal) {
        Long userId = getUserId(principal);
        if (userId == null) return "redirect:/login";
        postService.deleteComment(commentId, userId);
        return "redirect:/post/" + postId;
    }

    @PostMapping("/post/{id}/share")
    @ResponseBody
    public String sharePost(@PathVariable Long id) {
        postService.incrementShareCount(id);
        return "{\"success\": true, \"message\": \"转发成功！\"}";
    }
}
