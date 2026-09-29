package org.example.linkhub.repository;

import org.example.linkhub.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByTagName(String tagName);

    List<Tag> findByTagNameIn(Collection<String> tagNames);

    @Modifying
    @Query(value = "INSERT INTO t_tag (tag_name, created_at, updated_at) "
            + "VALUES (:name, now(), now()) ON CONFLICT (tag_name) DO NOTHING",
            nativeQuery = true)
    void upsertByName(@Param("name") String name);
}
