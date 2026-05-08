package com.example.jianlou.repository;

import com.example.jianlou.entity.Conversation;
import com.example.jianlou.entity.ConversationContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationContentRepository extends JpaRepository<ConversationContent, Long> {
    // 新增：按会话+商品ID+发送时间（增量）查询消息
    List<ConversationContent> findByOwnerAndGoodsIdAndSendTimeGreaterThanOrderBySendTimeAsc(
            Conversation owner, String goodsId, String sendTime);

    // 新增：按会话+商品ID查询所有消息
    List<ConversationContent> findByOwnerAndGoodsIdOrderBySendTimeAsc(
            Conversation owner, String goodsId);

    /**
     * 统计未读消息数量
     *
     @param conversation
     会话对象
     *
     @param receiver
     消息接收者（当前登录用户）
     *
     @param sendTime
     已读时间戳（本地存储的最后已读时间）
     *
     @return
     未读消息数量
     */
    long countByOwnerAndReceiverAndSendTimeGreaterThan(Conversation conversation, String receiver, String sendTime);
}