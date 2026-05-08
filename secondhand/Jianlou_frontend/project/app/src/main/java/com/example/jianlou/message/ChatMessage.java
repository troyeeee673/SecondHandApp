package com.example.jianlou.message;

import org.litepal.crud.DataSupport;

public class ChatMessage extends DataSupport {
    private String sessionId; // 新增：会话标识（如当前用户+对方用户）
    private String sender;     // 发送者账号
    private String receiver;   // 接收者账号（新增）
    private String content;
    private int type;
    private String time;

    // Getter/Setter方法
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}