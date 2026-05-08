package com.example.jianlou.entity;

import com.example.jianlou.entity.Goods;
import com.example.jianlou.entity.User;

import javax.persistence.*;
import java.util.Date;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "comment")
public class Comment {

    @Id
    private String commentId;

    @ManyToOne
    @JoinColumn(name = "goods_id")
    private Goods goods;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String content;

    private Date createTime;

    // 构造方法
    public Comment() {
    }

    public Comment(String commentId, Goods goods, User user, String content, Date createTime) {
        this.commentId = commentId;
        this.goods = goods;
        this.user = user;
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

    public Goods getGoods() {
        return goods;
    }

    public void setGoods(Goods goods) {
        this.goods = goods;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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



//    // equals 和 hashCode 方法（可选，但推荐基于主键实现）
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (o == null || getClass() != o.getClass()) return false;
//
//        Comment comment = (Comment) o;
//        return commentId != null ? commentId.equals(comment.commentId) : comment.commentId == null;
//    }
//
//    @Override
//    public int hashCode() {
//        return commentId != null ? commentId.hashCode() : 0;
//    }
}