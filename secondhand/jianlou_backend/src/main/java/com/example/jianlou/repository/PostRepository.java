package com.example.jianlou.repository;

import com.example.jianlou.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // 根据帖子ID查询
    Post findByPostId(String postId);
    // 根据帖子状态查询（正常/删除）
    List<Post> findByPostStatus(String postStatus);
    // 新增：按状态查询，并按发布时间倒序排列
    List<Post> findByPostStatusOrderByPostTimeDesc(String postStatus);
}