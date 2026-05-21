package com.example.jianlou.message;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class message_friend extends AppCompatActivity implements WebSocketManager.MessageListener {

    ImageView back;
    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_friend);
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    private void init() {
        back = findViewById(R.id.message_friend_back);
        progressBar = findViewById(R.id.message_friend_bar);
        back.setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.message_friend_RecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 注册WebSocket监听
        WebSocketManager.getInstance().addListener(this);

        // 确保WebSocket已连接，然后拉取会话列表
        if (WebSocketManager.getInstance().isConnected()) {
            loadConversationList();
        } else {
            WebSocketManager.getInstance().connect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WebSocketManager.getInstance().removeListener(this);
    }

    private void loadConversationList() {
        progressBar.setVisibility(View.VISIBLE);
        WebSocketManager.getInstance().pullConversationList();
    }

    // ============ WebSocket回调 ============

    @Override
    public void onConnected() {
        runOnUiThread(this::loadConversationList);
    }

    @Override
    public void onNewMessage(JSONObject messageJson) {
        // 收到新消息时，刷新会话列表（可能有新的最后一条消息）
        runOnUiThread(this::loadConversationList);
    }

    @Override
    public void onHistoryMessages(JSONArray messages) {
        // 会话列表页面不处理
    }

    @Override
    public void onConversationList(JSONArray conversations) {
        messageList.clear();
        try {
            for (int i = 0; i < conversations.length(); i++) {
                JSONObject json = conversations.getJSONObject(i);
                String username = json.getString("username");
                String user_name = json.getString("user_name");
                String content = json.getString("message");
                String goodsId = json.optString("goods_id", "");

                // 扩展Message类以支持goodsId
                Message msg = new Message(R.mipmap.shequ0, content, user_name, username);
                msg.setGoodsId(goodsId);
                messageList.add(msg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        runOnUiThread(() -> {
            messageAdapter = new MessageAdapter(messageList);
            recyclerView.setAdapter(messageAdapter);
            progressBar.setVisibility(View.GONE);
        });
    }

    @Override
    public void onSendAck(String sendTime, String status) {
        // 不处理
    }

    @Override
    public void onDisconnected() {
        runOnUiThread(() -> {
            Toast.makeText(message_friend.this, "消息服务已断开", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }

    @Override
    public void onError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(message_friend.this, "连接错误: " + error, Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
        });
    }

    // ============ 原有方法保持 ============

    private String getRadom() {
        Random random = new Random();
        int length = random.nextInt(10000) + 1;
        return String.valueOf(length);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = messageAdapter.getPosition();
        switch (item.getItemId()) {
            case 0:
                Toast.makeText(message_friend.this, "还没有开发", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                new AlertDialog.Builder(this).setMessage("确认删除该数据？")
                        .setPositiveButton("确定", (dialog, which) -> messageAdapter.removeData(position))
                        .setNegativeButton("取消", null)
                        .show();
                break;
        }
        return super.onContextItemSelected(item);
    }
}