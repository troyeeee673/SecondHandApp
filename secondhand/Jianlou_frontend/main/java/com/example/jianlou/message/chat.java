package com.example.jianlou.message;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.litepal.crud.DataSupport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class chat extends AppCompatActivity implements View.OnClickListener, WebSocketManager.MessageListener {

    private List<Msg> msgList = new ArrayList<>();
    private ImageView back, reload;
    private TextView friend_name;
    private EditText message;
    private Button send;
    private RecyclerView recyclerView;
    private MSgAdapter adapter;
    private String username;
    private String goodsId; // 新增：商品ID
    private ProgressBar progressBar;
    private String nowtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        init();
    }

    private void init() {
        back = findViewById(R.id.chat_back);
        friend_name = findViewById(R.id.chat_friend_name);
        message = findViewById(R.id.chat_message);
        send = findViewById(R.id.chat_send);
        reload = findViewById(R.id.chat_reload);
        progressBar = findViewById(R.id.chat_bar);

        reload.setOnClickListener(this);
        back.setOnClickListener(this);
        send.setOnClickListener(this);

        Intent intent = getIntent();
        username = intent.getStringExtra("username");
        goodsId = intent.getStringExtra("goodsId"); // 获取商品ID
        friend_name.setText(intent.getStringExtra("friend_name"));

        // 加载本地缓存消息
        List<ChatMessage> chatMessages = DataSupport.select("content", "type")
                .where("sender=?", username).order("time").find(ChatMessage.class);
        for (ChatMessage chatMessage : chatMessages) {
            msgList.add(new Msg(chatMessage.getContent(), chatMessage.getType(), R.mipmap.cat));
        }

        if (msgList.size() == 0) {
            nowtime = "";
        } else {
            List<ChatMessage> lastMsgs = DataSupport.select("time")
                    .where("sender=?", username).order("time desc").find(ChatMessage.class);
            if (lastMsgs.size() > 0) {
                nowtime = lastMsgs.get(0).getTime();
            }
        }

        recyclerView = findViewById(R.id.chat_recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MSgAdapter(msgList);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
                    }
                }
            }
        });

        // 注册WebSocket监听
        WebSocketManager.getInstance().addListener(this);

        // 通过WebSocket拉取历史消息
        if (WebSocketManager.getInstance().isConnected()) {
            WebSocketManager.getInstance().pullHistoryMessages(username, goodsId, nowtime);
        } else {
            // 如果WebSocket未连接，先连接
            WebSocketManager.getInstance().connect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WebSocketManager.getInstance().removeListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chat_back:
                finish();
                break;
            case R.id.chat_send:
                sendMessage();
                break;
            case R.id.chat_reload:
                if (WebSocketManager.getInstance().isConnected()) {
                    WebSocketManager.getInstance().pullHistoryMessages(username, goodsId, nowtime);
                }
                break;
        }
    }

    private void sendMessage() {
        String content = message.getText().toString().trim();
        if (content.isEmpty()) return;

        if (!WebSocketManager.getInstance().isConnected()) {
            Toast.makeText(this, "连接已断开，请重试", Toast.LENGTH_SHORT).show();
            return;
        }

        String sendTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        // 通过WebSocket发送消息
        WebSocketManager.getInstance().sendChatMessage(username, content, goodsId, sendTime);

        // 本地先显示消息（乐观更新）
        Msg msg = new Msg(content, Msg.TYPE_SEND, R.mipmap.cat);
        msgList.add(msg);
        adapter.notifyItemInserted(msgList.size() - 1);
        recyclerView.scrollToPosition(msgList.size() - 1);
        message.setText("");

        // 保存到本地数据库
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setContent(content);
        chatMessage.setType(Msg.TYPE_SEND);
        chatMessage.setTime(sendTime);
        chatMessage.setSender(StaticVar.username); // 当前登录用户
        chatMessage.save();
    }

    // ============ WebSocket回调 ============

    @Override
    public void onConnected() {
        runOnUiThread(() -> {
            Toast.makeText(chat.this, "已连接到消息服务", Toast.LENGTH_SHORT).show();
            // 连接成功后拉取历史消息
            WebSocketManager.getInstance().pullHistoryMessages(username, goodsId, nowtime);
        });
    }

    @Override
    public void onNewMessage(JSONObject messageJson) {
        try {
            String sender = messageJson.getString("sender");
            String content = messageJson.getString("message");
            String sendTime = messageJson.getString("sendTime");
            String msgGoodsId = messageJson.getString("goodsId");

            // 只处理当前商品的消息
            if (!msgGoodsId.equals(goodsId)) return;

            if (sender.equals(username)) {
                // 收到当前聊天对象的消息
                nowtime = sendTime;
                Msg msg = new Msg(content, Msg.TYPE_RECEIVED, R.mipmap.cat);
                msgList.add(msg);

                // 保存到本地
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setContent(content);
                chatMessage.setType(Msg.TYPE_RECEIVED);
                chatMessage.setTime(sendTime);
                chatMessage.setSender(sender);
                chatMessage.save();

                runOnUiThread(() -> {
                    adapter.notifyItemInserted(msgList.size() - 1);
                    recyclerView.scrollToPosition(msgList.size() - 1);
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onHistoryMessages(JSONArray messages) {
        try {
            // 清空现有列表，用服务器数据重建
            msgList.clear();
            for (int i = 0; i < messages.length(); i++) {
                JSONObject json = messages.getJSONObject(i);
                String sender = json.getString("sender");
                String content = json.getString("message");
                String sendTime = json.getString("send_time");

                nowtime = sendTime;

                int type = sender.equals(StaticVar.username) ? Msg.TYPE_SEND : Msg.TYPE_RECEIVED;
                msgList.add(new Msg(content, type, R.mipmap.cat));
            }

            runOnUiThread(() -> {
                adapter = new MSgAdapter(msgList);
                recyclerView.setAdapter(adapter);
                if (msgList.size() > 0) {
                    recyclerView.scrollToPosition(msgList.size() - 1);
                }
                progressBar.setVisibility(View.GONE);
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConversationList(JSONArray conversations) {
        // 聊天页面不需要处理会话列表
    }

    @Override
    public void onSendAck(String sendTime, String status) {
        // 消息发送确认，可以更新本地消息状态
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            Toast.makeText(chat.this, "消息服务已断开", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(chat.this, "连接错误: " + error, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }
}