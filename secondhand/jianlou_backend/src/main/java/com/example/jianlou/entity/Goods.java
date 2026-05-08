package com.example.jianlou.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "goods")
public class Goods {
    // 原有字段保持不变...
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_account", referencedColumnName = "account")
    private User owner;

    @Id
    @Column(length = 32, nullable = false, unique = true)
    private String hash;

    @Column(length = 300, nullable = false)
    private String content;

    @Column(length = 10, nullable = false)
    private String money;

    @Column(length = 10, nullable = false)
    private String originMoney;

    @Column(length = 10, nullable = false)
    private String sendMoney;

    @Column(length = 200, nullable = false)
    private String classify;

    @Column(name = "edit_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date editDate = new Date();

    // LY新增审核相关字段
    @Column(length = 20, columnDefinition = "varchar(20) default 'pending'")
    private String auditStatus = "pending";  // pending, approved, rejected

    @Column(length = 500)
    private String rejectReason;

    // YHT新增：商品状态字段
    @Column(name = "status", length = 20)
    private String status = "on_sale"; // 默认状态：在售

    // ========== 新增商品统计字段 ==========
    @Column(columnDefinition = "int default 0")
    private Integer totalNumber = 0;

    @Column(columnDefinition = "int default 0")
    private Integer returnNumber = 0;

    @Column(columnDefinition = "int default 0")
    private Integer soldNumber = 0;

    // 在Goods实体类中添加以下字段
    @Column(columnDefinition = "int default 0")
    private Integer likesCount = 0;  // 商品的点赞数

    // YHT常量定义
    public static final String STATUS_ON_SALE = "on_sale";      // 在售
    public static final String STATUS_SOLD = "sold";            // 已售出
    public static final String STATUS_OFF_SHELF = "off_shelf";  // 已下架
}