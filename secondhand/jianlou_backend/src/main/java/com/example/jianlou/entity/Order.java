package com.example.jianlou.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @Column(name = "order_id", length = 32, nullable = false, unique = true)
    private String orderId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "goods_hash", referencedColumnName = "hash")
    private Goods goods;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_account", referencedColumnName = "account")
    private User buyer;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_account", referencedColumnName = "account")
    private User seller;

    @Column(name = "create_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createTime = new Date();

    // 添加订单状态
    @Column(name = "status", length = 20)
    private String status = "pending"; // 默认待处理

    // 添加完成时间
    @Column(name = "complete_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completeTime;

    // 订单状态常量
    public static final String STATUS_PENDING = "pending";      // 待处理
    public static final String STATUS_COMPLETED = "completed";  // 已完成
    public static final String STATUS_RETURNED = "returned";    // 已退货
    public static final String STATUS_CANCELLED = "cancelled";  // 已取消
}