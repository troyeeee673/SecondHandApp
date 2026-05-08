package com.example.jianlou.shequ;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;

// 加上注解简化代码（若未使用lombok，可保留手动getter/setter）
@Data
@AllArgsConstructor
public class Post implements Serializable { // 关键：实现Serializable接口，支持Intent传递
    private static final long serialVersionUID = 1L; // 序列化版本号，避免序列化异常

    // 帖子唯一ID
    private String postId;
    // 帖子图片资源ID/网络地址
    private int postPhotoID;
    // 头像资源ID/网络地址
    private int postHeadID;
    private String postTitle;
    // 用户名
    private String postUser_name;
    // 点赞数
    private String postLove;
    // 话题标签
    private String postTalk;
    // 帖子内容
    private String postContent;
    // 来源社区
    private String postOrigin;
    // 网络图片地址（适配后端返回的图片路径）
    private String postPhotoUrl;
    private String postHeadUrl;

    // 手动getter/setter（若未使用lombok，保留以下代码）
    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public Post(){}
    // 补充：接收int资源ID的构造方法（匹配你的调用场景）
    public Post(int postPhotoID, int postHeadID, String postUser_name, String postLove, String postTalk, String postContent, String postOrigin) {
        this.postPhotoID = postPhotoID;
        this.postHeadID = postHeadID;
        this.postUser_name = postUser_name;
        this.postLove = postLove;
        this.postTalk = postTalk;
        this.postContent = postContent;
        this.postOrigin = postOrigin;
    }

    // 新增：适配后端真实数据的构造方法（后续替换模拟数据用）
    public Post(String postPhotoUrl, String postHeadUrl, String postUser_name, String postLove, String postTalk, String postContent, String postOrigin) {
        // 网络地址暂存为字符串（后续修改适配器支持网络图片）
        this.postPhotoID = -1; // 标记为网络图片
        this.postHeadID = -1; // 标记为网络图片
        this.postUser_name = postUser_name;
        this.postLove = postLove;
        this.postTalk = postTalk;
        this.postContent = postContent;
        this.postOrigin = postOrigin;
        // 新增网络地址字段
        this.postPhotoUrl = postPhotoUrl;
        this.postHeadUrl = postHeadUrl;
    }

    public String getPostPhotoUrl() {
        return postPhotoUrl;
    }

    public void setPostPhotoUrl(String postPhotoUrl) {
        this.postPhotoUrl = postPhotoUrl;
    }

    public int getPostHeadID() {
        return postHeadID;
    }

    public void setPostHeadID(int postHeadID) {
        this.postHeadID = postHeadID;
    }

    public String getPostUser_name() {
        return postUser_name;
    }

    public void setPostUser_name(String postUser_name) {
        this.postUser_name = postUser_name;
    }

    public int getPostPhotoID() {
        return postPhotoID;
    }

    public void setPostPhotoID(int postPhotoID) {
        this.postPhotoID = postPhotoID;
    }

    public String getPostLove() {
        return postLove;
    }

    public void setPostLove(String postLove) {
        this.postLove = postLove;
    }

    public String getPostTalk() {
        return postTalk;
    }

    public void setPostTalk(String postTalk) {
        this.postTalk = postTalk;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public String getPostOrigin() {
        return postOrigin;
    }

    public void setPostOrigin(String postOrigin) {
        this.postOrigin = postOrigin;
    }

    public String getPostHeadUrl() {
        return postHeadUrl;
    }

    public void setPostHeadUrl(String postHeadUrl) {
        this.postHeadUrl = postHeadUrl;
    }
}