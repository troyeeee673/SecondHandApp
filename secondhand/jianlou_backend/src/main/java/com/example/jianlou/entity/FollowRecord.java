package com.example.jianlou.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "follow_record")
public class FollowRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 关注者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_account", referencedColumnName = "account")
    private User follower;

    // 被关注者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "following_account", referencedColumnName = "account")
    private User following;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime = new Date();

    // 唯一约束：防止重复关注
    @Column(unique = true)
    private String uniqueKey; // follower_account + "_" + following_account
}