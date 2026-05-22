package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);

    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 批量删除某个帖子下的所有评论。
     * 使用 JPQL 批量 DELETE 避免 Hibernate 逐个加载实体导致的关联约束问题。
     */
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteAllByPostId(Long postId);

    void deleteByAuthorId(Long authorId);
}
