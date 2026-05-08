package com.example.jianlou.my;

import android.net.Uri;

public class Publish {
    private Uri photoID;
    private String content, money, goodID, status;

    // 修改构造函数，添加status参数
    public Publish(Uri photoImageID, String string_content, String string_money, String goodID, String status) {
        photoID = photoImageID;
        content = string_content;
        money = string_money;
        this.goodID = goodID;
        this.status = status;
    }

    // 保持原有的getter方法不变

    public Uri getPhotoID() {
        return photoID;
    }

    public String getContent() {
        return content;
    }

    public String getMoney() {
        return money;
    }

    public String getGoodID() {
        return goodID;
    }

    public String getStatus() {
        return status;
    }

    // 判断是否已售出
    public boolean isSold() {
        return "sold".equals(status);
    }
}