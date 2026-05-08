package com.example.jianlou.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "like_record")
public class LikeRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 点赞者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_account", referencedColumnName = "account")
    private User user;

    // 被点赞的商品
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_hash", referencedColumnName = "hash")
    private Goods goods;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime = new Date();

    // 唯一约束：防止重复点赞
    @Column(unique = true)
    private String uniqueKey; // user_account + "_" + goods_hash
}