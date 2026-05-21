package com.example.jianlou.websocket;

import com.example.jianlou.entity.Conversation;
import com.example.jianlou.entity.ConversationContent;
import com.example.jianlou.entity.User;
import com.example.jianlou.common.EncryptUtil;
import com.example.jianlou.repository.ConversationContentRepository;
import com.example.jianlou.repository.ConversationRepository;
import com.example.jianlou.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.*;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Resource
    private WebSocketSessionManager sessionManager;
    @Resource
    private ConversationRepository conversationRepository;
    @Resource
    private ConversationContentRepository conversationContentRepository;
    @Resource
    private UserRepository userRepository;
    @Resource
    private EncryptUtil encryptUtil;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String account = (String) session.getAttributes().get("account");
        if (account != null) {
            sessionManager.addSession(account, session);
            log.info("WebSocket连接建立: {}", account);
            sendMessage(session, createAckMessage("connected"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String account = (String) session.getAttributes().get("account");
        if (account == null) return;

        String payload = message.getPayload();
        Map<String, Object> msgMap = objectMapper.readValue(payload, Map.class);
        String type = (String) msgMap.get("type");

        switch (type) {
            case "chat":
                handleChatMessage(account, msgMap);
                break;
            case "pull_history":
                handlePullHistory(account, msgMap, session);
                break;
            case "pull_conversation_list":
                handlePullConversationList(account, session);
                break;
            case "ping":
                sendMessage(session, createPongMessage());
                break;
        }
    }

    private void handleChatMessage(String sender, Map<String, Object> msgMap) {
        String receiver = (String) msgMap.get("receiver");
        String message = (String) msgMap.get("message");
        String goodsId = (String) msgMap.get("goodsId");
        String sendTime = (String) msgMap.get("sendTime");

        try {
            String[] accounts = new String[]{sender, receiver};
            Arrays.sort(accounts);
            String conversationId = encryptUtil.md5hex(goodsId + "_" + accounts[0] + "_" + accounts[1]);

            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) {
                conversation = new Conversation();
                conversation.setId(conversationId);
                conversation.setSender(sender);
                conversation.setReceiver(receiver);
                conversation.setGoodsId(goodsId);
                conversationRepository.save(conversation);
            }

            ConversationContent content = new ConversationContent();
            content.setOwner(conversation);
            content.setSender(sender);
            content.setReceiver(receiver);
            content.setMessage(message);
            content.setGoodsId(goodsId);
            content.setSendTime(sendTime);
            conversationContentRepository.save(content);

            // 推送给接收者
            WebSocketSession receiverSession = sessionManager.getSession(receiver);
            if (receiverSession != null && receiverSession.isOpen()) {
                Map<String, Object> pushMsg = new HashMap<>();
                pushMsg.put("type", "new_message");
                pushMsg.put("sender", sender);
                pushMsg.put("message", message);
                pushMsg.put("goodsId", goodsId);
                pushMsg.put("sendTime", sendTime);
                sendMessage(receiverSession, pushMsg);
            }

            // 确认发送成功
            WebSocketSession senderSession = sessionManager.getSession(sender);
            if (senderSession != null && senderSession.isOpen()) {
                Map<String, Object> ack = new HashMap<>();
                ack.put("type", "send_ack");
                ack.put("sendTime", sendTime);
                ack.put("status", "success");
                sendMessage(senderSession, ack);
            }
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
        }
    }

    private void handlePullHistory(String account, Map<String, Object> msgMap, WebSocketSession session) {
        String friendUsername = (String) msgMap.get("friendUsername");
        String goodsId = (String) msgMap.get("goodsId");
        String time = (String) msgMap.get("time");

        try {
            String[] accounts = new String[]{account, friendUsername};
            Arrays.sort(accounts);
            String conversationId = encryptUtil.md5hex(goodsId + "_" + accounts[0] + "_" + accounts[1]);

            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            List<Map<String, String>> messages = new ArrayList<>();

            if (conversation != null) {
                List<ConversationContent> contents;
                if (time != null && !time.isEmpty()) {
                    contents = conversationContentRepository
                            .findByOwnerAndGoodsIdAndSendTimeGreaterThanOrderBySendTimeAsc(
                                    conversation, goodsId, time);
                } else {
                    contents = conversationContentRepository
                            .findByOwnerAndGoodsIdOrderBySendTimeAsc(conversation, goodsId);
                }

                for (ConversationContent msg : contents) {
                    Map<String, String> map = new HashMap<>();
                    map.put("sender", msg.getSender());
                    map.put("message", msg.getMessage());
                    map.put("send_time", msg.getSendTime());
                    messages.add(map);
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("type", "history_messages");
            response.put("messages", messages);
            sendMessage(session, response);
        } catch (Exception e) {
            log.error("拉取历史消息失败", e);
        }
    }

    private void handlePullConversationList(String account, WebSocketSession session) {
        try {
            List<Conversation> sendList = conversationRepository.findBySender(account);
            List<Conversation> receiveList = conversationRepository.findByReceiver(account);

            Set<String> idSet = new HashSet<>();
            List<Conversation> allConversations = new ArrayList<>();

            for (Conversation conv : sendList) {
                if (!idSet.contains(conv.getId())) {
                    idSet.add(conv.getId());
                    allConversations.add(conv);
                }
            }
            for (Conversation conv : receiveList) {
                if (!idSet.contains(conv.getId())) {
                    idSet.add(conv.getId());
                    allConversations.add(conv);
                }
            }

            List<Map<String, Object>> ret = new ArrayList<>();
            for (Conversation conv : allConversations) {
                String friendUsername = conv.getSender().equals(account)
                        ? conv.getReceiver() : conv.getSender();
                String goodsId = conv.getGoodsId();

                List<ConversationContent> contents = conversationContentRepository
                        .findByOwnerAndGoodsIdOrderBySendTimeAsc(conv, goodsId);

                if (contents.isEmpty()) continue;

                ConversationContent lastMsg = contents.get(contents.size() - 1);
                User friendUser = userRepository.findByAccount(friendUsername);

                Map<String, Object> map = new HashMap<>();
                map.put("username", friendUsername);
                map.put("user_name", friendUser != null ? friendUser.getUsername() : "未知用户");
                map.put("message", lastMsg.getMessage());
                map.put("datetime", lastMsg.getSendTime());
                map.put("goods_id", goodsId);
                ret.add(map);
            }

            ret.sort((m1, m2) -> m2.get("datetime").toString().compareTo(m1.get("datetime").toString()));

            Map<String, Object> response = new HashMap<>();
            response.put("type", "conversation_list");
            response.put("conversations", ret);
            sendMessage(session, response);
        } catch (Exception e) {
            log.error("拉取会话列表失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String account = (String) session.getAttributes().get("account");
        if (account != null) {
            sessionManager.removeSession(account);
            log.info("WebSocket连接关闭: {}", account);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket传输错误", exception);
    }

    private Map<String, Object> createAckMessage(String status) {
        Map<String, Object> ack = new HashMap<>();
        ack.put("type", "connection_ack");
        ack.put("status", status);
        return ack;
    }

    private Map<String, Object> createPongMessage() {
        Map<String, Object> pong = new HashMap<>();
        pong.put("type", "pong");
        return pong;
    }

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            if (session != null && session.isOpen()) {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("发送WebSocket消息失败", e);
        }
    }
}