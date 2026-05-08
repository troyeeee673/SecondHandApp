package com.example.jianlou.controller;

import com.example.jianlou.common.EncryptUtil;
import com.example.jianlou.entity.Conversation;
import com.example.jianlou.entity.ConversationContent;
import com.example.jianlou.entity.Cookie;
import com.example.jianlou.entity.User;
import com.example.jianlou.repository.ConversationContentRepository;
import com.example.jianlou.repository.ConversationRepository;
import com.example.jianlou.repository.CookieRepository;
import com.example.jianlou.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@CrossOrigin(origins = "*") // 解决跨域问题
public class ChatController {
    @Resource
    private ConversationRepository conversationRepository;
    @Resource
    private ConversationContentRepository conversationContentRepository;
    @Resource
    private CookieRepository cookieRepository;
    @Resource
    private UserRepository userRepository;
    @Resource
    private EncryptUtil encryptUtil;

    // 发送消息接口（绑定商品ID，区分不同商品聊天）
    @PostMapping("/push")
    public String chats(
            @RequestParam(value = "cookie", required = true) String cookie,
            @RequestParam(value = "Username", required = true) String username,
            @RequestParam(value = "message", required = true) String message,
            @RequestParam(value = "goodsId", required = true) String goodsId,
            @RequestParam(value = "sendTime", required = true) String sendTime) {
        System.out.println("===== 接收前端参数 =====");
        System.out.println("cookie: " + cookie);
        System.out.println("Username: " + username);
        System.out.println("message: " + message);
        System.out.println("goodsId: " + goodsId);

        try {
            // 1. 校验登录状态（通过cookie）
            Cookie c = cookieRepository.findByCookie(cookie);
            System.out.println("查询到的Cookie对象: " + (c == null ? "null" : c.toString()));
            if (c == null) {
                System.out.println("错误：cookie无效/未登录");
                return "failed: cookie无效";
            }
            String sender = c.getAccount(); // 当前登录用户（商家）账号
            String receiver = username;     // 客户账号
            System.out.println("发送者账号: " + sender);
            System.out.println("接收者账号: " + receiver);

            // 2. 生成唯一会话ID，对发送者和接收者账号排序，确保双方会话ID一致
            String[] accounts = new String[]{sender, receiver};
            Arrays.sort(accounts);
            String conversationId = encryptUtil.md5hex(goodsId + "_" + accounts[0] + "_" + accounts[1]);

            // 3. 查找/创建会话（绑定商品ID）
            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            System.out.println("查询到的会话: " + (conversation == null ? "null" : conversation.toString()));
            if (conversation == null) {
                conversation = new Conversation();
                conversation.setId(conversationId);
                conversation.setSender(sender);
                conversation.setReceiver(receiver);
                conversation.setGoodsId(goodsId); // 绑定商品ID
                conversationRepository.save(conversation);
                System.out.println("新建会话并保存成功");
            }

            // 4. 保存聊天消息（绑定商品ID+时间戳，避免长度截断）
            ConversationContent content = new ConversationContent();
            content.setOwner(conversation);
            content.setSender(sender);
            content.setReceiver(receiver);
            content.setMessage(message);
            content.setGoodsId(goodsId); // 绑定商品ID
            content.setSendTime(sendTime); // 时间戳（长度13，绝对不截断）
            conversationContentRepository.save(content);
            System.out.println("消息保存成功: " + content.toString());

            return "success";
        } catch (Exception e) {
            System.out.println("===== 接口报错 =====");
            e.printStackTrace();
            return "failed: " + e.getMessage();
        }
    }

    // 获取消息接口（按商品ID+用户过滤，仅返回对应商品的聊天记录）
    @PostMapping("/")
    public Object getMessage(
            @RequestParam(value = "cookie", required = true) String cookie,
            @RequestParam(value = "Username", required = true) String username,
            @RequestParam(value = "goodsId", required = true) String goodsId,
            @RequestParam(value = "time", required = false) String time) {
        try {
            // 1. 校验登录状态
            Cookie c = cookieRepository.findByCookie(cookie);
            if (c == null) {
                return new ArrayList<>();
            }
            String sender = c.getAccount();
            String receiver = username;

            // 2. 生成会话ID（匹配发送消息的会话ID）
            String[] accounts = new String[]{sender, receiver};
            Arrays.sort(accounts); // 新增：账号排序
            String conversationId = encryptUtil.md5hex(goodsId + "_" + accounts[0] + "_" + accounts[1]); // 修正：使用排序后的账号

            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) {
                return new ArrayList<>();
            }

            // 3. 查询该商品+该用户的消息（增量/全量）
            List<ConversationContent> messages;
            if (time != null && !time.isEmpty() && !"".equals(time)) {
                // 增量拉取：仅拉取最后一条消息之后的新消息
                messages = conversationContentRepository.findByOwnerAndGoodsIdAndSendTimeGreaterThanOrderBySendTimeAsc(
                        conversation, goodsId, time);
            } else {
                // 全量拉取：获取该商品的所有消息
                messages = conversationContentRepository.findByOwnerAndGoodsIdOrderBySendTimeAsc(
                        conversation, goodsId);
            }

            // 4. 组装返回数据（适配前端格式）
            List<Map<String, String>> ret = new ArrayList<>();
            for (ConversationContent msg : messages) {
                Map<String, String> map = new HashMap<>();
                map.put("sender", msg.getSender());
                map.put("message", msg.getMessage());
                map.put("send_time", msg.getSendTime());
                ret.add(map);
            }
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // 获取会话列表接口（按商品分组，显示所有商品的聊天会话）
    // 获取会话列表接口（按商品分组，显示所有商品的聊天会话，补充unread_count返回）
    @PostMapping("/friends")
    public Object getConversationList(
            @RequestParam(value = "cookie", required = true) String cookie,
            @RequestParam(value = "goodsId", required = false) String goodsId) {
        try {
            // 1. 校验登录状态
            Cookie c = cookieRepository.findByCookie(cookie);
            if (c == null) {
                return new ArrayList<>();
            }
            String currentUser = c.getAccount();

            // 2. 查询会话（可选：按商品ID过滤）
            List<Conversation> sendList, receiveList;
            if (goodsId != null && !goodsId.isEmpty()) {
                sendList = conversationRepository.findBySenderAndGoodsId(currentUser, goodsId);
                receiveList = conversationRepository.findByReceiverAndGoodsId(currentUser, goodsId);
            } else {
                sendList = conversationRepository.findBySender(currentUser);
                receiveList = conversationRepository.findByReceiver(currentUser);
            }

            // 3. 合并发送和接收的会话，避免重复
            Set<String> conversationIdSet = new HashSet<>(); // 用于去重
            List<Conversation> allConversations = new ArrayList<>();
            for (Conversation conv : sendList) {
                if (!conversationIdSet.contains(conv.getId())) {
                    conversationIdSet.add(conv.getId());
                    allConversations.add(conv);
                }
            }
            for (Conversation conv : receiveList) {
                if (!conversationIdSet.contains(conv.getId())) {
                    conversationIdSet
                            .add(conv.getId());
                    allConversations
                            .add(conv);
                }
            }

            // 4. 组装会话列表数据（包含商品ID+未读数量）
            List<Map<String, Object>> ret = new ArrayList<>();
            for (Conversation conversation : allConversations) {
                String friendUsername = conversation.getSender().equals(currentUser) ? conversation.getReceiver() : conversation.getSender();
                String goodsIdConv = conversation.getGoodsId();

                // 查询该会话的最后一条消息
                List<ConversationContent> contents = conversationContentRepository.findByOwnerAndGoodsIdOrderBySendTimeAsc(
                        conversation, goodsIdConv);
                if (contents.isEmpty()) {
                    continue;
                }
                ConversationContent lastMsg = contents.get(contents.size() - 1);

                // 查询该会话的未读数量（当前用户作为接收者，未读消息是发送时间大于本地已读时间，这里先默认传空字符串，统计全部未读）
                // 若需精准统计，可由前端传入已读时间戳，此处先返回总未读数量
                long unreadCount = conversationContentRepository.countByOwnerAndReceiverAndSendTimeGreaterThan(
                        conversation, currentUser, "");

                // 查询好友信息
                User friendUser = userRepository.findByAccount(friendUsername);

                Map<String, Object> map = new HashMap<>();
                map.put("username", friendUsername);
                map.put("user_name", friendUser != null ? friendUser.getUsername() : "未知用户");
                map.put("message", lastMsg.getMessage());
                map.put("datetime", lastMsg.getSendTime());
                map.put("goods_id", goodsIdConv);
                ret.add(map);
            }

            // 按时间倒序排序（最新消息在前）
            List<Map<String, Object>> finalRet = ret.stream()
                    .sorted((m1, m2) -> m2.get("datetime").toString().compareTo(m1.get("datetime").toString()))
                    .collect(Collectors.toList());
            return finalRet;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 查询单个会话的准确未读数量
     * @param cookie 登录凭证
     * @param friendUsername 好友账号
     * @param goodsId 商品ID
     * @param readTime 本地已读时间戳
     * @return 未读数量
     */
    @PostMapping("/unread/count")
    public Map<String, Object> getUnreadCount(
            @RequestParam("cookie") String cookie,
            @RequestParam("friendUsername") String friendUsername,
            @RequestParam("goodsId") String goodsId,
            @RequestParam(value = "readTime", defaultValue = "") String readTime) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 1. 校验登录
            Cookie c = cookieRepository.findByCookie(cookie);
            if (c == null) {
                result.put("code", -1);
                result.put("unreadCount", 0);
                return result;
            }
            String currentUser = c.getAccount();

            // 2. 生成会话ID
            String[] accounts = new String[]{currentUser, friendUsername};
            Arrays.sort(accounts);
            String conversationId = encryptUtil.md5hex(goodsId + "_" + accounts[0] + "_" + accounts[1]);
            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) {
                result.put("code", 0);
                result.put("unreadCount", 0);
                return result;
            }

            // 3. 统计未读数量
            long unreadCount = conversationContentRepository.countByOwnerAndReceiverAndSendTimeGreaterThan(
                    conversation, currentUser, readTime);

            result.put("code", 0);
            result.put("unreadCount", unreadCount);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", -1);
            result.put("unreadCount", 0);
            return result;
        }
    }
}