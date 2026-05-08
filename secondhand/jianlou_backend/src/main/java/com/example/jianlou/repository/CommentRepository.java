package com.example.jianlou.repository;


import com.example.jianlou.entity.Comment;
import com.example.jianlou.entity.Goods;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByGoodsOrderByCreateTimeDesc(Goods goods);
    void deleteByGoods(Goods goods);
}