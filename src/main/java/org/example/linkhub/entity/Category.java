package org.example.linkhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@SuperBuilder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "t_category")
//  - Category (分类表 - 自关联结构)
//      - 字段：id, name, description
//      - 关联：ManyToOne -> parent (指向父分类)
//      - 关联：OneToMany -> children (指向子分类列表)
//      - 关联：OneToMany -> Post (该分类下的所有帖子)
public class Category extends BaseEntity{
    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> children = new ArrayList<>();

    public void addChild (Category child){
        this.children.add(child);
        child.setParent(this);
    }

    @OneToMany(mappedBy = "category")
    private List<Post> posts = new ArrayList<>();
}
