package com.example.jianlou.repository;

import com.example.jianlou.entity.Post;
import com.example.jianlou.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {
    /**
     * 根据帖子查询关联的所有图片
     */
    List<PostImage> findByPost(Post post);

    /**
     * 根据帖子ID查询关联的所有图片
     */
    List<PostImage> findByPost_PostId(String postId);

    /**
     * 根据图片路径删除图片记录
     */
    void deleteByImagePath(String imagePath);
}