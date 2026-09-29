package org.example.linkhub.entity;
//- UserProfile (用户详情表)
//        - 字段：id, avatar, bio (简介), location
//        - 关联：OneToOne -> User (唯一外键 user_id)
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@Table(name = "t_user_profile")
public class UserProfile extends BaseEntity{

    @Column(name = "avatar", length = 255)
    private String avatar;

    @Column(name = "bio", columnDefinition = "text")
    private String bio;

    @Column(name = "location")
    private String location;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}
