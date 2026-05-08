package com.example.jianlou.message;

// 头像从本地资源id改为网络URL字符串
public class Message {
    private String headUrl; // 替换原有int headID
    private String message, user_name, username;
    private String goodsId; // 商品ID字段
    private int unreadCount;          // 新增：未读消息数量
    private String lastMsgTime;       // 新增：该会话最后一条消息时间戳（用于计算未读）

    // 构造函数修改：传入头像URL
    public Message(String headUrl, String string_message, String string_user_name, String username, String goodsId) {
        this.headUrl = headUrl == null ? "" : headUrl;
        this.message = string_message;
        this.user_name = string_user_name;
        this.username = username;
        this.goodsId = goodsId;
        this.unreadCount = 0; // 默认未读数为0
        this.lastMsgTime = ""; // 默认时间戳为空
    }

    public Message(String headUrl, String string_message, String string_user_name,  String username, String goodsId, int unreadCount, String lastMsgTime) {
        this.headUrl = headUrl == null ? "" : headUrl;
        this.message = string_message;
        this.user_name = string_user_name;
        this.username = username;
        this.goodsId = goodsId;
        this.unreadCount = unreadCount;
        this.lastMsgTime = lastMsgTime;
    }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    public String getLastMsgTime() { return lastMsgTime; }
    public void setLastMsgTime(String lastMsgTime) { this.lastMsgTime = lastMsgTime; }

    // 原有getter修改+新增goodsId getter
    public String getMessageHeadUrl() { return headUrl; } // 替换getMessageHeadID()
    public String getMessageUser_name() { return user_name; }
    public String getMessageMessage() { return message; }
    public String geMessagetUsername() { return username; }
    public String getGoodsId() { return goodsId; }
}