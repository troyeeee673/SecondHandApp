package com.example.jianlou.entity;

import javax.persistence.*;

@Entity
public class ConversationContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 消息ID

    private String sender;   // 发送者账号
    private String receiver; // 接收者账号
    private String message;  // 消息内容
    private String sendTime; // 发送时间（时间戳）

    // 新增：商品ID字段（关联商品）
    private String goodsId;

    // 关联会话
    @ManyToOne
    private Conversation owner;

    // ========== 构造方法 ==========
    public ConversationContent() {}

    // ========== get/set 方法 ==========
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSendTime() { return sendTime; }
    public void setSendTime(String sendTime) { this.sendTime = sendTime; }

    // 新增：goodsId的get/set
    public String getGoodsId() { return goodsId; }
    public void setGoodsId(String goodsId) { this.goodsId = goodsId; }

    public Conversation getOwner() { return owner; }
    public void setOwner(Conversation owner) { this.owner = owner; }
}