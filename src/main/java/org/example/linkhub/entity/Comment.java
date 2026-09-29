package org.example.linkhub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "t_comment")
//  - Comment (评论表)
//      - 字段：id, content, createTime
//      - 关联：ManyToOne -> Post (所属帖子)
//      - 关联：ManyToOne -> User (评论者)
public class Comment extends BaseEntity{
    @Column(name = "content",columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}
