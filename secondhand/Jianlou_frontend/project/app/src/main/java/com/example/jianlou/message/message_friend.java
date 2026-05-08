package com.example.jianlou.message;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.Util.TimeUtil;
import com.example.jianlou.staticVar.StaticVar;
import com.example.jianlou.staticVar.Table;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class message_friend extends AppCompatActivity {

    ImageView back;
    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;

    private Timer unreadRefreshTimer;
    private TimerTask unreadRefreshTask;

    /**
     * 保存会话的已读时间戳（会话标识=好友账号+商品ID）
     */
    public void saveReadTime(String friendUsername, String goodsId, String time) {
        SharedPreferences sp = getSharedPreferences("chat_read_status", Context.MODE_PRIVATE);
        String sessionKey = friendUsername + "_" + goodsId; // 唯一标识当前会话
        sp.edit().putString(sessionKey, time).apply();
    }

    /**
     * 获取会话的已读时间戳
     */
    public String getReadTime(String friendUsername, String goodsId) {
        SharedPreferences sp = getSharedPreferences("chat_read_status", Context.MODE_PRIVATE);
        String sessionKey = friendUsername + "_" + goodsId;
        return sp.getString(sessionKey, ""); // 无数据时返回空字符串
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_friend);
        init();
        // 启动未读数量定时刷新任务
        startUnreadRefreshTimer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消定时刷新任务，避免内存泄漏
        if (unreadRefreshTimer != null) {
            unreadRefreshTimer.cancel();
        }
    }

    private void init() {
        back = findViewById(R.id.message_friend_back);
        progressBar = findViewById(R.id.message_friend_bar);
        recyclerView = findViewById(R.id.message_friend_RecyclerView);

        // 1. 初始化 RecyclerView 布局管理器
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 2. 初始化适配器
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);

        // 绑定返回按钮事件
        back.setOnClickListener(v -> finish());
    }

    // 刷新RecyclerView数据
    private void updateRecycle() {
        runOnUiThread(() -> {
            messageAdapter.notifyDataSetChanged();
            recyclerView.scrollToPosition(messageList.size() - 1);
        });
    }

    // 加载会话列表
    private void initMessage() {
        progressBar.setVisibility(View.VISIBLE);
        messageList.clear();
        RequestBody requestBody = new FormBody.Builder()
                .add(Table.cookie, StaticVar.cookie)
                .build();
        HttpUtil.sendOkHttpRequest(StaticVar.friendUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateUI();
                outputMessage("请求失败，请检查网络");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                updateUI();
                response(response);
            }
        });
    }

    // 解析后端返回的会话数据
    private void response(Response response) throws IOException {
        if (response.code() == 200) {
            if (response.body() != null) {
                String responseData = response.body().string();
                if (responseData.equals("failed")) {
                    outputMessage("服务器错误");
                } else {
                    try {
                        JSONArray jsonArray = new JSONArray(responseData);
                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            String username = jsonObject.getString("username");
                            String content = jsonObject.getString("message");
                            String user_name = jsonObject.getString("user_name");
                            String goodsId = jsonObject.getString("goods_id");
                            String friendHeadUrl = jsonObject.optString("user_head", "");
                            String lastMsgTime = jsonObject.getString("datetime");

                            // 1. 优先获取后端返回的准确未读数量
                            long unreadCount = jsonObject.optLong("unread_count", 0);

                            // 构造Message对象
                            Message message = new Message(friendHeadUrl, content, user_name, username, goodsId, (int) unreadCount, lastMsgTime);
                            messageList.add(message);

                            // 请求单个会话的准确未读数量（若需要更精准）
                             requestAccurateUnreadCount(username, goodsId, i);
                        }
                        updateRecycle();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        outputMessage("解析消息列表失败");
                    }
                }
            }
        } else {
            outputMessage("服务器故障");
        }
    }

    /**
     * 本地计算未读数量（兼容后端未返回未读数量的场景）
     */
    private long calculateLocalUnreadCount(String friendUsername, String goodsId, String lastMsgTime) {
        String readTime = getReadTime(friendUsername, goodsId);
        if (TextUtils.isEmpty(readTime)) {
            return 1; // 从未读取，默认1条未读
        }
        // 若最后消息时间晚于已读时间，判定为1条未读（简化逻辑）
        return TimeUtil.isOver2Minutes(readTime, lastMsgTime) ? 1 : 0;
    }

    /**
     * 请求单个会话的准确未读数量（需后端配合提供/unread/count接口）
     */
    private void requestAccurateUnreadCount(String friendUsername, String goodsId, int position) {
        if (TextUtils.isEmpty(StaticVar.unreadCountUrl)) {
            outputMessage("未配置未读数量接口地址");
            return;
        }

        // 1. 获取本地已读时间戳
        String readTime = getReadTime(friendUsername, goodsId);

        // 2. 构建请求体
        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("friendUsername", friendUsername)
                .add("goodsId", goodsId)
                .add("readTime", readTime)
                .build();

        // 3. 发送请求
        HttpUtil.sendOkHttpRequest(StaticVar.unreadCountUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(message_friend.this, "获取未读数量失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        int code = jsonObject.getInt("code");
                        if (code == 0) {
                            long unreadCount = jsonObject.getLong("unreadCount");
                            // 运行在UI线程，更新该会话的未读数量
                            runOnUiThread(() -> {
                                if (position < messageList.size()) {
                                    Message message = messageList.get(position);
                                    message.setUnreadCount((int) unreadCount);
                                    messageAdapter.notifyItemChanged(position);
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 启动未读数量定时刷新任务
     */
    private void startUnreadRefreshTimer() {
        unreadRefreshTimer = new Timer();
        unreadRefreshTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    // 每5秒刷新一次会话列表（可调整间隔）
                    initMessage();
                });
            }
        };
        // 延迟1秒执行，每5秒刷新一次
        unreadRefreshTimer.schedule(unreadRefreshTask, 1000, 5000);
    }

    /**
     * 获得随机数（备用方法，可保留）
     */
    private String getRadom() {
        Random random = new Random();
        int length = random.nextInt(10000) + 1;
        return String.valueOf(length);
    }

    /**
     * 响应上下文菜单事件
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = messageAdapter.getPosition();
        switch (item.getItemId()) {
            case 0:
                Toast.makeText(message_friend.this, "还没有开发", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                new AlertDialog.Builder(this).setMessage("确认删除该数据？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            messageAdapter.removeData(position);
                        })
                        .setNegativeButton("取消", null)
                        .show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    // 更新UI（隐藏进度条）
    private void updateUI() {
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
    }

    // 显示Toast提示（修复Looper异常问题）
    private void outputMessage(String message) {
        runOnUiThread(() -> Toast.makeText(message_friend.this, message, Toast.LENGTH_SHORT).show());
    }
}