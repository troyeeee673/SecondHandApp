package com.example.jianlou.index;

import java.util.Date;

public class Comment {
    private String commentId;
    private String goodsId;
    private String userId;
    private String userName;
    private String content;
    private Date createTime;

    // 构造方法
    public Comment(String commentId, String goodsId, String userId, String userName,
                   String content, Date createTime) {
        this.commentId = commentId;
        this.goodsId = goodsId;
        this.userId = userId;
        this.userName = userName;
        this.content = content;
        this.createTime = createTime;
    }

    // getter 和 setter 方法
    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}