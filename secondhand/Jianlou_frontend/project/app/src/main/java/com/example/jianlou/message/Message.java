package com.example.jianlou.message;

public class Message {
    private int headID;
    private String message, user_name, username;
    private String goodsId; // 新增

    public Message(int headImageID, String string_message, String string_user_name, String username) {
        headID = headImageID;
        message = string_message;
        user_name = string_user_name;
        this.username = username;
    }

    public int getMessageHeadID() {
        return headID;
    }

    public String getMessageUser_name() {
        return user_name;
    }

    public String getMessageMessage() {
        return message;
    }

    public String geMessagetUsername() {
        return username;
    }

    public String getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId;
    }
}