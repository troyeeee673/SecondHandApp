package com.example.jianlou.message;

// 消息实体类
public class Msg {
    // 布局类型常量
    public static final int TYPE_RECEIVED = 0;    // 接收消息
    public static final int TYPE_SEND = 1;        // 发送消息
    public static final int TYPE_TIME = 2;        // 时间戳

    private String content;       // 消息内容（仅消息类型有效）
    private int type;             // 布局类型（消息/时间）
    private String headUrl;       // 头像URL（仅消息类型有效）
    private String sendTime;      // 消息时间戳（13位毫秒数，所有类型有效）
    private String formatTime;    // 格式化后的时间（如「2025-12-20 16:30」，仅时间类型有效）

    // 消息类型构造方法
    public Msg(String content, int type, String headUrl, String sendTime) {
        this.content = content;
        this.type = type;
        this.headUrl = headUrl == null ? "" : headUrl;
        this.sendTime = sendTime;
        this.formatTime = "";
    }

    // 时间戳类型构造方法
    public Msg(String formatTime, String sendTime) {
        this.content = "";
        this.type = TYPE_TIME;
        this.headUrl = "";
        this.sendTime = sendTime;
        this.formatTime = formatTime;
    }

    // Getter方法
    public String getContent() { return content; }
    public int getType() { return type; }
    public String getHeadUrl() { return headUrl; }
    public String getSendTime() { return sendTime; }
    public String getFormatTime() { return formatTime; }
}
