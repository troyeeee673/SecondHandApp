package com.example.jianlou.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.util.List;

@Entity
public class Conversation {
    @Id
    private String id; // 会话ID（商品ID+双方账号的MD5）

    private String sender;   // 发送者账号
    private String receiver; // 接收者账号

    // 新增：商品ID字段（关联商品）
    private String goodsId;

    // 关联消息列表（可选）
    @OneToMany(mappedBy = "owner")
    private List<ConversationContent> contents;

    // ========== 构造方法 ==========
    public Conversation() {}

    // ========== get/set 方法 ==========
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    // 新增：goodsId的get/set
    public String getGoodsId() { return goodsId; }
    public void setGoodsId(String goodsId) { this.goodsId = goodsId; }

    public List<ConversationContent> getContents() { return contents; }
    public void setContents(List<ConversationContent> contents) { this.contents = contents; }
}