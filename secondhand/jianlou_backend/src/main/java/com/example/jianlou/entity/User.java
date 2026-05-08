package com.example.jianlou.entity;

import lombok.Data;
import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @Column(length = 11, nullable = false, unique = true)
    private String account; //账号

    @Column(length = 32, nullable = false)
    private String password; //密码

    @Column(length = 50)
    private String username;

    @Column(length = 2, columnDefinition = "varchar(2) default '未知'")
    private String sex = "未知";

    @Column
    private Date birthday = java.sql.Date.valueOf("2025-12-24");

    @Column(length = 50)
    private String location;

    @Column(length = 100, columnDefinition = "TEXT")
    private String introduction;

    @Column(length = 10)
    private String school;

    // 头像路径（对应ImageField）
    @Column
    private String head;

    // 用户状态（pending待审核/approved已通过/rejected拒绝），默认待审核
    @Column(length = 20, columnDefinition = "varchar(20) default 'pending'")
    private String userStatus = "pending";

    // 用户类型（user/admin），默认普通用户
    @Column(length = 20, columnDefinition = "varchar(20) default 'user'")
    private String userType = "user";

    // 用户等级相关字段
    @Column(columnDefinition = "int default 0")
    private Integer activeScore = 0;

    @Column(columnDefinition = "int default 60")
    private Integer creditScore = 60;

    @Column(length = 20, columnDefinition = "varchar(20) default '青铜'")
    private String level = "青铜";

    @Column(columnDefinition = "int default 0")
    private Integer transactionCount = 0;

    @Column(columnDefinition = "int default 0")
    private Integer reviewCount = 0;

    @Column(columnDefinition = "decimal(5,2) default 100.00")
    private Double successRate = 100.00;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastCalcTime;

    @Column(columnDefinition = "int default 0")
    private Integer levelScore = 0;

    // 用户互动功能
    @Column(columnDefinition = "int default 0")
    private Integer likesReceived = 0;  // 收到的总赞数

    @Column(columnDefinition = "int default 0")
    private Integer followersCount = 0;  // 粉丝数

    @Column(columnDefinition = "int default 0")
    private Integer followingCount = 0;  // 关注数

    // 在User实体类中添加
    public String getAvatar() {
        return this.head; // 返回head字段的值
    }

    public void setAvatar(String avatar) {
        this.head = avatar; // 设置到head字段
    }

}