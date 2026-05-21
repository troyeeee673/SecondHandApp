package com.example.jianlou.message;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.jianlou.staticVar.StaticVar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketManager {

    private static final String TAG = "WebSocketManager";
    private static final String WS_URL = StaticVar.wsUrl; // 如: ws://your-server/ws/chat?cookie=xxx

    private static WebSocketManager instance;
    private WebSocket webSocket;
    private OkHttpClient client;
    private Handler mainHandler;
    private boolean isConnected = false;

    private List<MessageListener> listeners = new ArrayList<>();

    public interface MessageListener {
        void onConnected();
        void onNewMessage(JSONObject message);
        void onHistoryMessages(JSONArray messages);
        void onConversationList(JSONArray conversations);
        void onSendAck(String sendTime, String status);
        void onDisconnected();
        void onError(String error);
    }

    private WebSocketManager() {
        mainHandler = new Handler(Looper.getMainLooper());
        client = new OkHttpClient.Builder()
                .readTimeout(0, TimeUnit.MILLISECONDS) // 长连接不超时
                .build();
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    public void addListener(MessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public boolean isConnected() {
        return isConnected && webSocket != null;
    }

    /**
     * 连接WebSocket
     */
    public void connect() {
        if (isConnected) return;

        String url = WS_URL + "?cookie=" + StaticVar.cookie;
        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                isConnected = true;
                Log.d(TAG, "WebSocket连接成功");
                mainHandler.post(() -> {
                    for (MessageListener listener : listeners) {
                        listener.onConnected();
                    }
                });
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d(TAG, "收到消息: " + text);
                try {
                    JSONObject json = new JSONObject(text);
                    String type = json.optString("type");
                    mainHandler.post(() -> dispatchMessage(type, json));
                } catch (JSONException e) {
                    Log.e(TAG, "解析消息失败", e);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                Log.d(TAG, "WebSocket正在关闭: " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                isConnected = false;
                Log.d(TAG, "WebSocket已关闭: " + reason);
                mainHandler.post(() -> {
                    for (MessageListener listener : listeners) {
                        listener.onDisconnected();
                    }
                });
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                isConnected = false;
                Log.e(TAG, "WebSocket连接失败", t);
                mainHandler.post(() -> {
                    for (MessageListener listener : listeners) {
                        listener.onError(t.getMessage());
                    }
                });
            }
        });
    }

    private void dispatchMessage(String type, JSONObject json) {
        for (MessageListener listener : listeners) {
            try {
                switch (type) {
                    case "new_message":
                        listener.onNewMessage(json);
                        break;
                    case "history_messages":
                        JSONArray historyMessages = json.optJSONArray("messages");
                        if (historyMessages != null) {
                            listener.onHistoryMessages(historyMessages);
                        }
                        break;
                    case "conversation_list":
                        JSONArray conversations = json.optJSONArray("conversations");
                        if (conversations != null) {
                            listener.onConversationList(conversations);
                        }
                        break;
                    case "send_ack":
                        listener.onSendAck(json.optString("sendTime"), json.optString("status"));
                        break;
                    case "connection_ack":
                        // 连接确认，已在onOpen中处理
                        break;
                    case "pong":
                        // 心跳响应
                        break;
                }
            } catch (Exception e) {
                Log.e(TAG, "分发消息异常", e);
            }
        }
    }

    /**
     * 发送聊天消息
     */
    public void sendChatMessage(String receiver, String message, String goodsId, String sendTime) {
        if (!isConnected) {
            Log.e(TAG, "WebSocket未连接");
            return;
        }
        try {
            JSONObject json = new JSONObject();
            json.put("type", "chat");
            json.put("receiver", receiver);
            json.put("message", message);
            json.put("goodsId", goodsId);
            json.put("sendTime", sendTime);
            webSocket.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "构建消息失败", e);
        }
    }

    /**
     * 拉取历史消息
     */
    public void pullHistoryMessages(String friendUsername, String goodsId, String time) {
        if (!isConnected) return;
        try {
            JSONObject json = new JSONObject();
            json.put("type", "pull_history");
            json.put("friendUsername", friendUsername);
            json.put("goodsId", goodsId);
            json.put("time", time != null ? time : "");
            webSocket.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "构建拉取请求失败", e);
        }
    }

    /**
     * 拉取会话列表
     */
    public void pullConversationList() {
        if (!isConnected) return;
        try {
            JSONObject json = new JSONObject();
            json.put("type", "pull_conversation_list");
            webSocket.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "构建拉取请求失败", e);
        }
    }

    /**
     * 发送心跳
     */
    private void sendPing() {
        if (!isConnected) return;
        try {
            JSONObject json = new JSONObject();
            json.put("type", "ping");
            webSocket.send(json.toString());
        } catch (JSONException e) {
            Log.e(TAG, "发送心跳失败", e);
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "用户主动断开");
            isConnected = false;
        }
    }
}