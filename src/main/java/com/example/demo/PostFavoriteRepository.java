package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Long> {

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    void deleteAllByPostId(Long postId);

    List<PostFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
