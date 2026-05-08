package com.example.jianlou.repository;

import com.example.jianlou.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    // 原有方法
    List<Conversation> findBySenderAndGoodsId(String sender, String goodsId);
    List<Conversation> findByReceiverAndGoodsId(String receiver, String goodsId);

    // 新增：按发送者查询会话
    List<Conversation> findBySender(String sender);

    // 新增：按接收者查询会话
    List<Conversation> findByReceiver(String receiver);
}