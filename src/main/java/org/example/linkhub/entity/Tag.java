package org.example.linkhub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

//  - Tag (标签表)
//      - 字段：id, tagName (唯一索引)
//      - 关联：ManyToMany -> Post (被 Post 维护)
@SuperBuilder
@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@Entity
@Table(name = "t_tag")
public class Tag extends BaseEntity{

    @Column(name = "tag_name", nullable = false, unique = true)
    private String tagName;

    @ManyToMany(mappedBy = "tags")
    private Set<Post> posts = new HashSet<>();
}
