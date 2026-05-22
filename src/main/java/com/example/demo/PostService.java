package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostFavoriteRepository postFavoriteRepository;
    private final UserRepository userRepository;

    public PostService(PostRepository postRepository, CommentRepository commentRepository,
                       PostLikeRepository postLikeRepository, PostFavoriteRepository postFavoriteRepository,
                       UserRepository userRepository) {
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.postLikeRepository = postLikeRepository;
        this.postFavoriteRepository = postFavoriteRepository;
        this.userRepository = userRepository;
    }

    // ==================== Post CRUD ====================

    public Post createPost(Long authorId, String title, String content, String imageUrls, String location) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Post post = new Post();
        post.setAuthor(author);
        post.setTitle(title);
        post.setContent(content);
        post.setImageUrls(imageUrls);
        post.setLocation(location);
        return postRepository.save(post);
    }

    public Optional<Post> findById(Long id) {
        return postRepository.findById(id);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (!post.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权删除他人帖子");
        }
        // 先清理关联数据，避免外键约束
        postLikeRepository.deleteAllByPostId(postId);
        postFavoriteRepository.deleteAllByPostId(postId);
        commentRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
    }

    public Page<PostView> getAllPosts(int page, int size, Long currentUserId) {
        Set<Long> likedIds = currentUserId != null
                ? new HashSet<>(postRepository.findLikedPostIdsByUserId(currentUserId)) : Collections.emptySet();
        Set<Long> favIds = currentUserId != null
                ? new HashSet<>(postRepository.findFavoritedPostIdsByUserId(currentUserId)) : Collections.emptySet();

        return postRepository.findByStatusOrderByCreatedAtDesc(Post.Status.APPROVED, PageRequest.of(page, size))
                .map(post -> toPostView(post, likedIds.contains(post.getId()), favIds.contains(post.getId())));
    }

    public Page<PostView> searchPosts(String keyword, int page, int size, Long currentUserId) {
        Set<Long> likedIds = currentUserId != null
                ? new HashSet<>(postRepository.findLikedPostIdsByUserId(currentUserId)) : Collections.emptySet();
        Set<Long> favIds = currentUserId != null
                ? new HashSet<>(postRepository.findFavoritedPostIdsByUserId(currentUserId)) : Collections.emptySet();

        return postRepository.findByStatusAndTitleContainingOrStatusAndContentContainingOrderByCreatedAtDesc(
                        Post.Status.APPROVED, keyword, Post.Status.APPROVED, keyword, PageRequest.of(page, size))
                .map(post -> toPostView(post, likedIds.contains(post.getId()), favIds.contains(post.getId())));
    }

    public List<PostView> getPostsByUser(Long authorId, Long currentUserId) {
        Set<Long> likedIds = currentUserId != null
                ? new HashSet<>(postRepository.findLikedPostIdsByUserId(currentUserId)) : Collections.emptySet();
        Set<Long> favIds = currentUserId != null
                ? new HashSet<>(postRepository.findFavoritedPostIdsByUserId(currentUserId)) : Collections.emptySet();

        return postRepository.findByAuthorIdOrderByCreatedAtDesc(authorId).stream()
                .map(post -> toPostView(post, likedIds.contains(post.getId()), favIds.contains(post.getId())))
                .collect(Collectors.toList());
    }

    public List<PostView> getMyFavorites(Long userId) {
        return postFavoriteRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(pf -> {
                    Post post = pf.getPost();
                    if (post.getStatus() != Post.Status.APPROVED) return null;
                    return toPostView(post,
                            postLikeRepository.existsByUserIdAndPostId(userId, post.getId()),
                            true);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== Like ====================

    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (postLikeRepository.existsByUserIdAndPostId(userId, postId)) {
            postLikeRepository.deleteByUserIdAndPostId(userId, postId);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
            return false;
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            postLikeRepository.save(new PostLike(user, post));
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
            return true;
        }
    }

    // ==================== Comment ====================

    @Transactional
    public Comment addComment(Long postId, Long userId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        Comment comment = new Comment(post, user, content);
        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.save(post);
        return commentRepository.save(comment);
    }

    public List<Comment> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
    }

    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("无权删除他人评论");
        }
        Post post = comment.getPost();
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
        commentRepository.delete(comment);
    }

    // ==================== Favorite ====================

    @Transactional
    public boolean toggleFavorite(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        if (postFavoriteRepository.existsByUserIdAndPostId(userId, postId)) {
            postFavoriteRepository.deleteByUserIdAndPostId(userId, postId);
            post.setFavoriteCount(Math.max(0, post.getFavoriteCount() - 1));
            postRepository.save(post);
            return false;
        } else {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));
            postFavoriteRepository.save(new PostFavorite(user, post));
            post.setFavoriteCount(post.getFavoriteCount() + 1);
            postRepository.save(post);
            return true;
        }
    }

    // ==================== Share ====================

    @Transactional
    public void incrementShareCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        post.setShareCount(post.getShareCount() + 1);
        postRepository.save(post);
    }

    // ==================== Stats ====================

    public long countByUser(Long userId) {
        return postRepository.countByAuthorId(userId);
    }

    public boolean isPostLiked(Long postId, Long userId) {
        return postLikeRepository.existsByUserIdAndPostId(userId, postId);
    }

    public boolean isPostFavorited(Long postId, Long userId) {
        return postFavoriteRepository.existsByUserIdAndPostId(userId, postId);
    }

    // ==================== Admin Review ====================

    @Transactional
    public void approvePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        post.setStatus(Post.Status.APPROVED);
        postRepository.save(post);
    }

    @Transactional
    public void rejectPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        post.setStatus(Post.Status.REJECTED);
        postRepository.save(post);
    }

    public Page<Post> getPendingPosts(int page, int size) {
        return postRepository.findByStatusOrderByCreatedAtDesc(Post.Status.PENDING, PageRequest.of(page, size));
    }

    public Page<Post> getAllPostsWithStatus(int page, int size) {
        return postRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    public long countPendingPosts() {
        return postRepository.countByStatus(Post.Status.PENDING);
    }

    // ==================== Admin Delete ====================

    @Transactional
    public void adminDeletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("帖子不存在"));
        postLikeRepository.deleteAllByPostId(postId);
        postFavoriteRepository.deleteAllByPostId(postId);
        commentRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
    }

    @Transactional
    public void adminDeleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("评论不存在"));
        Post post = comment.getPost();
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        postRepository.save(post);
        commentRepository.delete(comment);
    }

    public List<Comment> getAllComments(int page, int size) {
        Page<Comment> result = commentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return result.getContent();
    }

    public long countAllComments() {
        return commentRepository.count();
    }

    // ==================== Helper ====================

    private PostView toPostView(Post post, boolean liked, boolean favorited) {
        return new PostView(post, liked, favorited);
    }

    // ==================== View Classes ====================

    public static class PostView {
        private final Post post;
        private final boolean liked;
        private final boolean favorited;

        public PostView(Post post, boolean liked, boolean favorited) {
            this.post = post;
            this.liked = liked;
            this.favorited = favorited;
        }

        public Post getPost() { return post; }
        public boolean isLiked() { return liked; }
        public boolean isFavorited() { return favorited; }

        public String getAuthorName() {
            if (post.getAuthor() == null) return "未知";
            String name = post.getAuthor().getName();
            return (name != null && !name.isEmpty()) ? name : post.getAuthor().getUsername();
        }

        public String getAuthorAvatarUrl() {
            if (post.getAuthor() == null) return null;
            return post.getAuthor().getAvatarUrl();
        }

        public String getPreviewContent() {
            String c = post.getContent();
            return c != null && c.length() > 120 ? c.substring(0, 120) + "..." : c;
        }

        public List<String> getImageList() {
            if (post.getImageUrls() == null || post.getImageUrls().isBlank()) return Collections.emptyList();
            return Arrays.asList(post.getImageUrls().split(","));
        }
    }
}
