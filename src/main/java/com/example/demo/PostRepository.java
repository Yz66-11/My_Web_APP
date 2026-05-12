package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByStatusOrderByCreatedAtDesc(Post.Status status, Pageable pageable);

    Page<Post> findByTitleContainingOrContentContainingOrderByCreatedAtDesc(String title, String content, Pageable pageable);

    Page<Post> findByStatusAndTitleContainingOrStatusAndContentContainingOrderByCreatedAtDesc(
            Post.Status status1, String title, Post.Status status2, String content, Pageable pageable);

    long countByAuthorId(Long authorId);

    long countByStatus(Post.Status status);

    @Query("SELECT p.id FROM PostLike pl JOIN pl.post p WHERE pl.user.id = :userId")
    List<Long> findLikedPostIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT p.id FROM PostFavorite pf JOIN pf.post p WHERE pf.user.id = :userId")
    List<Long> findFavoritedPostIdsByUserId(@Param("userId") Long userId);
}
