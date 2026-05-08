package com.example.jianlou.message;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.Util.TimeUtil;
import com.example.jianlou.staticVar.StaticVar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

/*聊天实现界面*/
public class chat extends AppCompatActivity implements View.OnClickListener {

    // 核心UI控件
    private List<Msg> msgList = new ArrayList<>();
    private ImageView chat_back, chat_reload;
    private TextView chat_friend_name;
    private EditText chat_message;
    private Button chat_send;
    private RecyclerView chat_recycler_view;
    private MSgAdapter msgAdapter;
    private ProgressBar chat_bar;

    // 业务参数
    private String targetUsername;    // 客户账号
    private String currentUserAccount;// 商家账号
    private String currentUserHeadUrl;// 当前用户头像URL
    private String goodsId;           // 当前聊天商品ID
    private String lastMsgTime = "";  // 最后一条消息时间戳

    // 定时拉取任务
    private Timer timer;
    private TimerTask timerTask;
    private Handler handler = new Handler();
    private boolean isTimerRunning = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // 先加载用户信息（确保account最新）
        StaticVar.loadUserInfo(this);

        // 初始化基础参数（登录+商品/用户信息）
        if (!initBaseParams()) {
            finish();
            return;
        }

        // 初始化UI控件
        initView();

        // 加载该商品的所有聊天记录
        loadAllMessages();

        // 启动定时拉取新消息
        startTimerTask();
    }

    /**
     * 初始化基础参数（校验登录+解析跳转参数）
     */
    private boolean initBaseParams() {
        // 校验登录状态
        if (StaticVar.cookie.isEmpty()) {
            Toast.makeText(this, "未登录，请先登录", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 解析跳转参数
        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(this, "跳转参数错误", Toast.LENGTH_SHORT).show();
            return false;
        }

        targetUsername = intent.getStringExtra("username");
        goodsId = intent.getStringExtra("goodsId");
        String friendName = intent.getStringExtra("friend_name");

        // 校验参数完整性
        if (targetUsername == null || targetUsername.isEmpty() || goodsId == null || goodsId.isEmpty()) {
            Toast.makeText(this, "缺少聊天对象/商品信息", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 直接从StaticVar获取账号和头像
        currentUserAccount = StaticVar.account;
        currentUserHeadUrl = StaticVar.headUrl;
        if (currentUserAccount.isEmpty()) {
            currentUserAccount = "unknown";
        }

        // 调试信息
        System.out.println("当前商家账号: " + currentUserAccount);
        System.out.println("客户账号: " + targetUsername);
        System.out.println("商品ID: " + goodsId);

        return true;
    }


    /**
     * 初始化UI控件+绑定事件
     */
    private void initView() {
        // 绑定控件
        chat_back = findViewById(R.id.chat_back);
        chat_reload = findViewById(R.id.chat_reload);
        chat_friend_name = findViewById(R.id.chat_friend_name);
        chat_message = findViewById(R.id.chat_message);
        chat_send = findViewById(R.id.chat_send);
        chat_bar = findViewById(R.id.chat_bar);
        chat_recycler_view = findViewById(R.id.chat_recycler_view);

        // 设置点击事件
        chat_back.setOnClickListener(this);
        chat_reload.setOnClickListener(this);
        chat_send.setOnClickListener(this);

        // 设置客户昵称
        String friendName = getIntent().getStringExtra("friend_name");
        chat_friend_name.setText(friendName == null ? targetUsername : friendName);

        // 初始化消息列表
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chat_recycler_view.setLayoutManager(layoutManager);
        msgAdapter = new MSgAdapter(msgList);
        chat_recycler_view.setAdapter(msgAdapter);

        // 滑动隐藏软键盘
        chat_recycler_view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideSoftInput();
                }
            }
        });
    }

    /**
     * 加载该商品的所有聊天记录
     */
    private void loadAllMessages() {
        chat_bar.setVisibility(View.VISIBLE);

        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("Username", targetUsername)
                .add("goodsId", goodsId)
                .add("time", "")
                .build();

        HttpUtil.sendOkHttpRequest(StaticVar.chatUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    chat_bar.setVisibility(View.GONE);
                    Toast.makeText(chat.this, "加载消息失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> chat_bar.setVisibility(View.GONE));

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    parseMessageResponse(responseData, true);
                    // 全量加载后立即标记已读
                    markCurrentSessionAsRead();
                } else {
                    runOnUiThread(() -> Toast.makeText(chat.this, "加载消息失败：服务器错误", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    /**
     * 加载增量消息（最后一条之后的新消息）
     */
    private void loadNewMessages() {
        if (lastMsgTime.isEmpty()) return;

        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("Username", targetUsername)
                .add("goodsId", goodsId)
                .add("time", lastMsgTime)
                .build();

        HttpUtil.sendOkHttpRequest(StaticVar.chatUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string();
                    parseMessageResponse(responseData, false);
                    // 增量加载新消息后，立即标记已读（当前界面已查看）
                    markCurrentSessionAsRead();
                }
            }
        });
    }

    /**
     * 解析后端返回的消息数据
     */
    private void parseMessageResponse(String responseData, boolean isRefreshAll) {
        try {
            JSONArray jsonArray = new JSONArray(responseData);
            if (jsonArray.length() == 0) return;

            // 全量刷新时清空列表
            if (isRefreshAll) {
                runOnUiThread(() -> msgList.clear());
            }

            // 记录上一条消息的时间戳
            String lastMsgTimeStamp = "";
            // 获取当前列表最后一条消息的时间（增量刷新时使用）
            if (!isRefreshAll && !msgList.isEmpty()) {
                lastMsgTimeStamp = msgList.get(msgList.size() - 1).getSendTime();
            }

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String sender = jsonObject.getString("sender");
                String content = jsonObject.getString("message");
                String sendTime = jsonObject.getString("send_time");
                String senderHeadUrl = jsonObject.optString("user_head", "");

                lastMsgTime = sendTime; // 更新最后一条消息时间戳

                // 1. 判断是否需要插入时间戳
                boolean needShowTime = TimeUtil.isOver2Minutes(lastMsgTimeStamp, sendTime);
                if (needShowTime) {
                    // 格式化时间
                    String formatTime = TimeUtil.formatTime(sendTime);
                    // 插入时间戳消息
                    Msg timeMsg = new Msg(formatTime, sendTime);
                    runOnUiThread(() -> {
                        msgList.add(timeMsg);
                    });
                }

                // 2. 构建并添加当前消息
                int msgType = sender.equals(currentUserAccount) ? Msg.TYPE_SEND : Msg.TYPE_RECEIVED;
                String targetHeadUrl = (msgType == Msg.TYPE_SEND) ? currentUserHeadUrl : senderHeadUrl;
                Msg msg = new Msg(content, msgType, targetHeadUrl, sendTime);

                runOnUiThread(() -> {
                    msgList.add(msg);
                    msgAdapter.notifyDataSetChanged();
                    chat_recycler_view.scrollToPosition(msgList.size() - 1);
                });

                // 3. 更新上一条消息时间戳
                lastMsgTimeStamp = sendTime;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(chat.this, "解析消息失败", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * 发送消息到后端
     */
    private void sendMessage() {
        String content = chat_message.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "消息内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        // 1. 本地生成当前时间戳
        String currentTimeStamp = String.valueOf(System.currentTimeMillis());
        // 2. 判断是否需要插入时间戳
        String lastTime = "";
        if (!msgList.isEmpty()) {
            lastTime = msgList.get(msgList.size() - 1).getSendTime();
        }
        boolean needShowTime = TimeUtil.isOver2Minutes(lastTime, currentTimeStamp);
        // 记录是否插入了时间戳（用于失败回滚）
        final boolean finalNeedShowTime = needShowTime;

        if (needShowTime) {
            String formatTime = TimeUtil.formatTime(currentTimeStamp);
            Msg timeMsg = new Msg(formatTime, currentTimeStamp);
            msgList.add(timeMsg);
        }
        // 3. 插入本地发送消息（先显示，提升体验）
        Msg sendMsg = new Msg(content, Msg.TYPE_SEND, currentUserHeadUrl, currentTimeStamp);
        msgList.add(sendMsg);
        msgAdapter.notifyDataSetChanged();
        chat_recycler_view.scrollToPosition(msgList.size() - 1);

        // 4. 后续网络请求逻辑（原有代码不变，仅删除loadNewMessages()）
        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("Username", targetUsername)
                .add("message", content)
                .add("goodsId", goodsId)
                .add("sendTime", currentTimeStamp) // 传递本地时间戳给后端
                .build();

        chat_bar.setVisibility(View.VISIBLE);
        HttpUtil.sendOkHttpRequest(StaticVar.pushUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    chat_bar.setVisibility(View.GONE);
                    Toast.makeText(chat.this, "发送失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // 失败时移除本地插入的消息
                    msgList.remove(msgList.size() - 1);
                    if (finalNeedShowTime) {
                        msgList.remove(msgList.size() - 1);
                    }
                    msgAdapter.notifyDataSetChanged();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> chat_bar.setVisibility(View.GONE));

                if (response.isSuccessful() && response.body() != null) {
                    String result = response.body().string();
                    runOnUiThread(() -> {
                        if (result.contains("success")) {
                            chat_message.setText("");
                            Toast.makeText(chat.this, "发送成功", Toast.LENGTH_SHORT).show();
                            // 优化：更新lastMsgTime，避免定时任务重复拉取
                            lastMsgTime = currentTimeStamp;
                            // 发送消息后标记已读
                            markCurrentSessionAsRead();
                        } else {
                            Toast.makeText(chat.this, "发送失败：" + result, Toast.LENGTH_SHORT).show();
                            // 失败时移除本地插入的消息
                            msgList.remove(msgList.size() - 1);
                            if (finalNeedShowTime) {
                                msgList.remove(msgList.size() - 1);
                            }
                            msgAdapter.notifyDataSetChanged();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(chat.this, "发送失败：服务器错误", Toast.LENGTH_SHORT).show();
                        // 失败时移除本地插入的消息
                        msgList.remove(msgList.size() - 1);
                        if (finalNeedShowTime) {
                            msgList.remove(msgList.size() - 1);
                        }
                        msgAdapter.notifyDataSetChanged();
                    });
                }
            }
        });
    }

    /**
     * 启动定时拉取任务
     */
    private void startTimerTask() {
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (isTimerRunning) {
                    handler.post(chat.this::loadNewMessages);
                }
            }
        };
        timer.schedule(timerTask, 1000, 5000);
    }

    /**
     * 隐藏软键盘
     */
    private void hideSoftInput() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), 0);
        }
    }

    /**
     * 标记当前会话为已读（保存最新lastMsgTime到SP）
     */
    private void markCurrentSessionAsRead() {
        if (TextUtils.isEmpty(targetUsername) || TextUtils.isEmpty(goodsId) || TextUtils.isEmpty(lastMsgTime)) {
            return;
        }
        // 直接操作SharedPreferences，无需强转上下文
        SharedPreferences sp = getSharedPreferences("chat_read_status", Context.MODE_PRIVATE);
        String sessionKey = targetUsername + "_" + goodsId;
        sp.edit().putString(sessionKey, lastMsgTime).apply();
        System.out.println("标记已读：会话Key=" + sessionKey + "，已读时间=" + lastMsgTime);
    }

    /**
     * 点击事件处理
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.chat_back) {
            isTimerRunning = false;
            // 退出前再次标记已读，确保最新消息被记录
            markCurrentSessionAsRead();
            finish();
        } else if (id == R.id.chat_send) {
            sendMessage();
        } else if (id == R.id.chat_reload) {
            loadAllMessages();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isTimerRunning = false;
        if (timer != null) timer.cancel();
        if (timerTask != null) timerTask.cancel();
        handler.removeCallbacksAndMessages(null);
        // 销毁前最终标记已读
        markCurrentSessionAsRead();
    }

}