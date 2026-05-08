package com.example.jianlou.index;

import android.net.Uri;

public class Good {
    /**
     * 商品类，就是首页的商品的显示的类
     */
    private Uri photoID;          // 商品图片Uri
    private int headID;           // 本地头像资源ID
    private String content, money, user_name, goodID;
    private String username;    //发布者账号字段


    // 构造方法
    public Good(Uri photoImageID, int headImageID, String string_content, String string_money, String string_user_name, String goodID) {
        photoID = photoImageID;
        headID = headImageID;
        content = string_content;
        money = string_money;
        user_name = string_user_name;
        this.goodID = goodID;
    }

    public Uri getPhotoID() {
        return photoID;
    }

    public int getHeadID() {
        return headID;
    }

    public String getContent() {
        return content;
    }

    public String getMoney() {
        return money;
    }

    public String getUser_name() {
        return user_name;
    }

    public String getGoodID() {
        return goodID;
    }

    // 发布者账号
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}