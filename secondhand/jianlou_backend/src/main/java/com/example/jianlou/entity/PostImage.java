package com.example.jianlou.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "post_image") // 数据库表名：帖子图片表
@Data // 自动生成getter/setter/toString，若未使用lombok需手动编写
public class PostImage {
    /**
     * 主键ID（自增）
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的帖子（多对一：多张图片对应一个帖子）
     */
    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false) // 关联帖子的post_id字段，非空
    private Post post;

    /**
     * 图片存储路径（相对路径，如：upload/post/f5136cc3/xxx.jpeg）
     */
    @Column(name = "image_path", nullable = false)
    private String imagePath;

    /**
     * 图片访问URL（完整路径，如：http://192.168.34.31:8080/upload/post/f5136cc3/xxx.jpeg）
     */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * 图片原始名称
     */
    @Column(name = "original_name")
    private String originalName;
}