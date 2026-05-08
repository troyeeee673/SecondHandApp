package com.example.jianlou.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "goods_image")
public class GoodsImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 外键关联Goods
    @ManyToOne
    @JoinColumn(name = "goods_hash", referencedColumnName = "hash")
    private Goods goods;

    // 商品图片路径
    @Column
    private String image;
}