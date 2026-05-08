package com.example.jianlou.entity;

import lombok.Data;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Data
@Entity
@Table(name = "post")
public class Post {
    @Id
    @Column(name = "post_id")
    private String postId; // 帖子唯一ID

    @ManyToOne
    @JoinColumn(name = "user_account", nullable = false)
    private User owner; // 发帖用户

    @Column(name = "post_title", nullable = false)
    private String postTitle; // 帖子标题

    @Column(name = "post_content", nullable = false, length = 2000)
    private String postContent; // 帖子内容

    @Column(name = "post_type")
    private String postType; // 帖子类型

    @Column(name = "post_community")
    private String postCommunity;

    @Column(name = "post_time")
    private Date postTime; // 发帖时间

    @Column(name = "post_status")
    private String postStatus = "normal"; // 帖子状态：normal-正常，deleted-删除

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PostImage> postImages; // 帖子图片列表
}