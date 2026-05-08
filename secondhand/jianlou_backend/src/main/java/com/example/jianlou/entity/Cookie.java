package com.example.jianlou.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "cookie")
public class Cookie {
    // 主键自增
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Cookie值：唯一约束
    @Column(name = "cookie", length = 64, nullable = false, unique = true)
    private String cookie;

    // 时间
    @Column(name = "change_time", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date changeTime;

    // 账号：非空约束，长度11（匹配手机号格式）
    @Column(name = "account", length = 11, nullable = false)
    private String account;

    // 构造方法：初始化修改时间，避免空值
    public Cookie() {
        this.changeTime = new Date();
    }
}