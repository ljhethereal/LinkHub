package org.example.linkhub.repository;


import org.example.linkhub.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    boolean existsByCategoryId(Long categoryId);

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN p.tags t " +
            "WHERE (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "                        OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
            "AND (:tagName IS NULL OR LOWER(t.tagName) = LOWER(:tagName)) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> search(@Param("keyword") String keyword,
                      @Param("categoryId") Long categoryId,
                      @Param("tagName") String tagName,
                      Pageable pageable);

    @Modifying
    @Transactional
    @Query("update Post p set p.views = coalesce(p.views, 0) + :delta where p.id = :id")
    int incrementViews(@Param("id") Long id, @Param("delta") Long delta);

    @Query("select coalesce(p.views, 0) from Post p where p.id = :id")
    Optional<Long> findViewsById(@Param("id") Long id);
}
