package com.example.jianlou.index;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.message.chat;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jianlou.R;
import com.example.jianlou.my.CircleTransform;
import com.example.jianlou.staticVar.StaticVar;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class good_detail extends AppCompatActivity implements View.OnClickListener {
    private String goodsID;
    ImageView back, chat, head;
    TextView user_name, money, content, origin_money, send_money, time;
    TextView tvCommentTitle, tvCommentCount, tvNoComments;
    EditText etComment;
    Button btnSendComment, btnBuyNow; // 新增立即购买按钮
    RecyclerView recyclerView, rvComments;
    private ProgressBar progressBar;
    // 全局唯一图片列表
    private final List<Photo> photoList = new ArrayList<>();
    private final List<Comment> commentList = new ArrayList<>();
    private String username;
    private static final String BACKEND_BASE_URL = "http://192.168.34.31:8080";
    private PhotoAdapter photoAdapter;
    private CommentAdapter commentAdapter;

    // 添加价格变量和卖家信息
    private String currentPrice = "0"; // 当前商品价格
    private String currentContent = ""; // 商品描述
    private String sellerAccount = ""; // 卖家账号
    private String sellerName = ""; // 卖家昵称

    // ========== 新增：点赞和关注相关变量 ==========
    private Button followBtn;
    private LinearLayout likeContainer;
    private ImageView likeIcon;
    private TextView likeCountText;

    // 点赞和关注状态
    private boolean isFollowing = false;
    private boolean isLiked = false;
    private int likeCount = 0;
    private String sellerUserId = ""; // 卖家的用户ID，用于关注功能

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_good_detail);
        goodsID = getIntent().getStringExtra("goodsID");
        // 加载用户信息，确保 StaticVar.account 最新
        StaticVar.loadUserInfo(this);
        init();
    }

    private void init() {
        // 绑定商品详情控件
        progressBar = findViewById(R.id.good_detail_bar);
        back = findViewById(R.id.good_detail_back);
        origin_money = findViewById(R.id.good_detail_origin_money);
        send_money = findViewById(R.id.good_detail_send_money);
        chat = findViewById(R.id.good_detail_user_message);
        chat.setVisibility(View.GONE);
        user_name = findViewById(R.id.good_detail_user_name);
        money = findViewById(R.id.good_detail_money);
        content = findViewById(R.id.good_detail_content);
        recyclerView = findViewById(R.id.good_detail_recycle);
        head = findViewById(R.id.good_detail_photo);
        time = findViewById(R.id.good_detail_time);

        // 绑定评论相关控件
        tvCommentTitle = findViewById(R.id.tv_comment_title);
        tvCommentCount = findViewById(R.id.tv_comment_count);
        tvNoComments = findViewById(R.id.tv_no_comments);
        etComment = findViewById(R.id.et_comment);
        btnSendComment = findViewById(R.id.btn_send_comment);
        rvComments = findViewById(R.id.rv_comments);

        // 绑定立即购买按钮
        btnBuyNow = findViewById(R.id.btn_buy_now);

        // ========== 新增：绑定点赞和关注控件 ==========
        followBtn = findViewById(R.id.good_detail_follow_btn);
        likeContainer = findViewById(R.id.good_detail_like_container);
        likeIcon = findViewById(R.id.good_detail_like_icon);
        likeCountText = findViewById(R.id.good_detail_like_count);

        // 文本样式设置
        origin_money.getPaint().setFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        money.getPaint().setFlags(Paint.FAKE_BOLD_TEXT_FLAG);

        // 绑定点击事件
        back.setOnClickListener(this);
        chat.setOnClickListener(this);
        btnSendComment.setOnClickListener(this);
        btnBuyNow.setOnClickListener(this); // 绑定购买按钮点击

        // ========== 新增：绑定点赞和关注点击事件 ==========
        followBtn.setOnClickListener(this);
        likeContainer.setOnClickListener(this);

        // 初始化图片RecyclerView（保留你原本的适配器，不新增任何布局）
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        photoAdapter = new PhotoAdapter(photoList);
        recyclerView.setAdapter(photoAdapter);

        // 初始化评论RecyclerView（保留你原本的适配器）
        LinearLayoutManager commentLayoutManager = new LinearLayoutManager(this);
        rvComments.setLayoutManager(commentLayoutManager);
        commentAdapter = new CommentAdapter(commentList);
        rvComments.setAdapter(commentAdapter);

        // 加载商品详情和评论
        initPhoto();
        loadComments();
    }

    /**
     * 更新点赞UI状态
     */
    private void updateLikeUI() {
        runOnUiThread(() -> {
            if (isLiked) {
                likeIcon.setImageResource(R.drawable.ic_like_filled);
                likeContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_liked_bg));
                likeCountText.setTextColor(ContextCompat.getColor(this, R.color.myred));
            } else {
                likeIcon.setImageResource(R.drawable.ic_like_empty);
                likeContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_like_bg));
                likeCountText.setTextColor(ContextCompat.getColor(this, R.color.color_999999));
            }
            likeCountText.setText(String.valueOf(likeCount));
        });
    }

    /**
     * 更新关注UI状态
     */
    private void updateFollowUI() {
        runOnUiThread(() -> {
            if (isFollowing) {
                followBtn.setText("已关注");
                followBtn.setTextColor(ContextCompat.getColor(this, R.color.color_999999));
                followBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_followed_bg));
            } else {
                followBtn.setText("关注");
                followBtn.setTextColor(ContextCompat.getColor(this, R.color.white));
                followBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_follow_bg));
            }
        });
    }

    /**
     * 处理点赞点击事件 - 修正版
     */
    private void handleLikeClick() {
        // 检查登录状态
        if (StaticVar.cookie.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否是自己发布的商品
        StaticVar.loadUserInfo(this);
        String currentUserAccount = StaticVar.user_name;
        if (sellerAccount.equals(currentUserAccount)) {
            Toast.makeText(this, "不能给自己发布的商品点赞", Toast.LENGTH_SHORT).show();
            return;
        }

        // ========== 修正：使用 /like/toggle/ 接口 ==========
        String likeUrl = BACKEND_BASE_URL + "/like/toggle/";

        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("goodsID", goodsID)
                .build();

        HttpUtil.sendOkHttpRequest(likeUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(good_detail.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.code() == 200 && response.body() != null) {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        boolean success = jsonObject.optBoolean("success", false);
                        String message = jsonObject.optString("message", "");

                        if (success) {
                            boolean liked = jsonObject.optBoolean("liked", false);
                            int count = jsonObject.optInt("like_count", 0);  // 获取最新的总点赞数

                            runOnUiThread(() -> {
                                isLiked = liked;
                                likeCount = count;  // ✅ 更新总点赞数
                                updateLikeUI();

                                if (isLiked) {
                                    Toast.makeText(good_detail.this, "点赞成功", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(good_detail.this, "已取消点赞", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(good_detail.this, "操作失败: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(good_detail.this, "服务器错误: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(good_detail.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * 处理关注点击事件 - 修正版
     */
    private void handleFollowClick() {
        // 检查登录状态
        if (StaticVar.cookie.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否是自己
        StaticVar.loadUserInfo(this);
        String currentUserAccount = StaticVar.user_name;
        if (sellerAccount.equals(currentUserAccount)) {
            Toast.makeText(this, "不能关注自己", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查是否有卖家用户ID
        if (sellerUserId.isEmpty()) {
            Toast.makeText(this, "用户信息不完整", Toast.LENGTH_SHORT).show();
            return;
        }

        // ========== 修正：使用 /follow/toggle/ 接口 ==========
        String followUrl = BACKEND_BASE_URL + "/follow/toggle/";

        // ========== 修正：参数名改为 targetAccount ==========
        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("targetAccount", sellerUserId)  // 修改参数名
                .build();

        HttpUtil.sendOkHttpRequest(followUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(good_detail.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.code() == 200 && response.body() != null) {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);
                        boolean success = jsonObject.optBoolean("success", false);
                        String message = jsonObject.optString("message", "");

                        if (success) {
                            // ========== 修正：从服务器响应中获取最新状态 ==========
                            boolean following = jsonObject.optBoolean("following", false);

                            runOnUiThread(() -> {
                                isFollowing = following;
                                updateFollowUI();

                                if (isFollowing) {
                                    Toast.makeText(good_detail.this, "关注成功", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(good_detail.this, "已取消关注", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(good_detail.this, "操作失败: " + message, Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(good_detail.this, "服务器错误: " + response.code(), Toast.LENGTH_SHORT).show();
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(good_detail.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void initPhoto() {
        photoList.clear();
        photoAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.VISIBLE);

        RequestBody requestBody = new FormBody.Builder()
                .add("goodsID", goodsID)
                .build();

        HttpUtil.sendOkHttpRequest(StaticVar.detailUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(good_detail.this, "请求失败，请检查网络", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(response);
            }
        });
    }

    /**
     * 加载评论
     */
    private void loadComments() {
        RequestBody requestBody = new FormBody.Builder()
                .add("goodsID", goodsID)
                .build();

        // 假设评论接口为 /comments/get/
        String commentUrl = BACKEND_BASE_URL + "/comments/get/";

        HttpUtil.sendOkHttpRequest(commentUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(good_detail.this, "加载评论失败", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String responseData = response.body().string();
                    parseComments(responseData);
                }
            }
        });
    }

    /**
     * 解析评论数据
     */
    private void parseComments(String responseData) {
        runOnUiThread(() -> {
            try {
                if (responseData.equals("failed") || responseData.equals("[]")) {
                    // 没有评论
                    updateCommentUI(0);
                    return;
                }

                JSONArray jsonArray = new JSONArray(responseData);
                commentList.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String commentId = jsonObject.optString("commentId", "");
                    String goodsId = jsonObject.optString("goodsId", "");
                    String userId = jsonObject.optString("userId", "");
                    String userName = jsonObject.optString("userName", "匿名用户");
                    String commentContent = jsonObject.optString("content", "");
                    String timeStr = jsonObject.optString("createTime", "");

                    // 解析时间
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    Date createTime;
                    try {
                        createTime = sdf.parse(timeStr);
                    } catch (Exception e) {
                        createTime = new Date();
                    }

                    Comment comment = new Comment(commentId, goodsId, userId, userName, commentContent, createTime);
                    commentList.add(comment);
                }

                commentAdapter.setCommentList(commentList);
                updateCommentUI(commentList.size());

            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(good_detail.this, "评论数据解析失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 更新评论UI
     */
    private void updateCommentUI(int count) {
        tvCommentCount.setText("(" + count + "条)");

        if (count > 0) {
            rvComments.setVisibility(View.VISIBLE);
            tvNoComments.setVisibility(View.GONE);
        } else {
            rvComments.setVisibility(View.GONE);
            tvNoComments.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 发送评论
     */
    private void sendComment() {
        String commentContent = etComment.getText().toString().trim();

        if (TextUtils.isEmpty(commentContent)) {
            Toast.makeText(this, "评论内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (StaticVar.cookie.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 构建评论请求
        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("goodsID", goodsID)
                .add("content", commentContent)
                .build();

        String commentUrl = BACKEND_BASE_URL + "/comments/add/";

        HttpUtil.sendOkHttpRequest(commentUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(good_detail.this, "发送失败，请检查网络", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 先获取响应体和响应码
                int responseCode = response.code();
                String responseData = null;

                try {
                    if (response.body() != null) {
                        responseData = response.body().string();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                final String finalResponseData = responseData;
                final int finalResponseCode = responseCode;

                runOnUiThread(() -> {
                    if (finalResponseCode == 200 && finalResponseData != null) {
                        if (finalResponseData.trim().equals("success")) {
                            // 清空输入框
                            etComment.setText("");

                            // 创建本地评论对象
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                            Comment comment = new Comment("", goodsID, "", "我", commentContent, new Date());
                            commentList.add(0, comment); // 添加到顶部
                            commentAdapter.notifyItemInserted(0);
                            updateCommentUI(commentList.size());

                            Toast.makeText(good_detail.this, "评论发布成功", Toast.LENGTH_SHORT).show();

                            // 可选：重新从服务器加载评论以确保数据一致性
                            // loadComments();
                        } else {
                            Toast.makeText(good_detail.this, "发布失败：" + finalResponseData, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(good_detail.this, "服务器错误：" + finalResponseCode, Toast.LENGTH_SHORT).show();
                    }

                    // 确保响应体被关闭
                    if (response.body() != null) {
                        response.body().close();
                    }
                });
            }
        });
    }

    private void handleResponse(Response response) throws IOException {
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));

        if (response.code() != 200) {
            runOnUiThread(() ->
                    Toast.makeText(good_detail.this, "服务器故障：" + response.code(), Toast.LENGTH_SHORT).show()
            );
            return;
        }

        String responseData = response.body() == null ? "" : response.body().string();
        if ("failed".equals(responseData) || responseData.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(good_detail.this, "服务器返回异常数据", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(responseData);

            username = jsonObject.optString("username", "");
            String userName = jsonObject.optString("user_name", "未知用户");
            String originMoney = jsonObject.optString("origin_money", "0");
            String sendMoney = jsonObject.optString("send_money", "0");
            String moneyStr = jsonObject.optString("money", "0");
            String contentStr = jsonObject.optString("content", "暂无描述");
            String timeStr = jsonObject.optString("time", "");

            // ========== 新增：从商品详情获取点赞数和卖家ID ==========
            likeCount = jsonObject.optInt("like_count", 0);
            sellerUserId = jsonObject.optString("seller_user_id", username); // 卖家用户ID

            // 保存卖家信息
            sellerAccount = jsonObject.optString("seller_account", username); // 保存卖家账号
            sellerName = userName; // 保存卖家昵称

            // 保存价格和描述到成员变量
            currentPrice = moneyStr;
            currentContent = contentStr;

            // 解析图片
            JSONArray imagesArray = jsonObject.optJSONArray("images");
            photoList.clear();

            if (imagesArray != null && imagesArray.length() > 0) {
                for (int i = 0; i < imagesArray.length(); i++) {
                    JSONObject imgObj = imagesArray.getJSONObject(i);
                    String imagePath = imgObj.optString("image", "");
                    String finalImgUrl = getFinalImageUrl(imagePath);
                    photoList.add(new Photo(Uri.parse(finalImgUrl)));
                }
            }

            runOnUiThread(() -> {
                user_name.setText(userName);
                money.setText(moneyStr);
                content.setText(contentStr);
                send_money.setText("(" + sendMoney + ")");
                origin_money.setText(originMoney);
                time.setText("发布于:" + timeStr);

                Picasso.get()
                        .load(R.mipmap.cat)
                        .transform(new CircleTransform())
                        .into(head);

                photoAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(photoAdapter);

                // ========== 新增：初始化点赞按钮状态 ==========
                likeCountText.setText(String.valueOf(likeCount));

                updateLikeUI();

                // ===================== 控制聊天图标显示/隐藏 =====================
                // 1. 先获取当前登录账号（从StaticVar中）
                String currentLoginAccount = StaticVar.account;
                // 2. 校验卖家账号和当前登录账号（避免空指针）
                boolean isSelfPublished = false;
                if (!TextUtils.isEmpty(sellerAccount) && !TextUtils.isEmpty(currentLoginAccount)) {
                    // 对比：卖家账号 == 当前登录账号 → 是本账号发布的商品
                    isSelfPublished = sellerAccount.equals(currentLoginAccount);
                }
                // 3. 设置聊天图标可见性：本账号发布则隐藏，否则显示
                if (isSelfPublished) {
                    chat.setVisibility(View.GONE); // 隐藏聊天图标
                    // ========== 修改：用户本人的商品也显示点赞，但不可点击 ==========
                    followBtn.setVisibility(View.GONE);
                    likeContainer.setVisibility(View.VISIBLE);
                    likeContainer.setEnabled(false);  // 禁止本人点赞
                    likeContainer.setAlpha(0.5f);     // 设置半透明效果
                    likeIcon.setAlpha(0.5f);          // 图标也半透明
                    likeCountText.setAlpha(0.5f);     // 数字也半透明

                    // 显示当前点赞数（用户本人可以看到）
                    likeCountText.setText(String.valueOf(likeCount));
                    updateLikeUI();

                    // 用户本人不能点赞，所以isLiked始终为false（即使之前误操作过）
                    isLiked = false;

                } else {
                    chat.setVisibility(View.VISIBLE); // 显示聊天图标
                    // ========== 修改：其他人的商品显示可点击的点赞 ==========
                    followBtn.setVisibility(View.VISIBLE);
                    likeContainer.setVisibility(View.VISIBLE);
                    likeContainer.setEnabled(true);   // 允许点赞
                    likeContainer.setAlpha(1.0f);     // 正常透明度
                    likeIcon.setAlpha(1.0f);
                    likeCountText.setAlpha(1.0f);

                    // ========== 新增：加载点赞和关注状态 ==========
                    loadLikeAndFollowStatus();
                }

// 更新点赞数显示（无论是否本人）
                likeCountText.setText(String.valueOf(likeCount));
                updateLikeUI();
            });

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(good_detail.this, "数据解析失败：" + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    /**
     * 加载点赞和关注状态
     */
    private void loadLikeAndFollowStatus() {
        if (StaticVar.cookie.isEmpty()) {
            // 用户未登录，不加载点赞和关注状态
            return;
        }

        // 加载点赞状态
        loadLikeStatus();

        // 加载关注状态
        if (!sellerUserId.isEmpty()) {
            loadFollowStatus();
        }
    }

    /**
     * 加载点赞状态 - 修正版
     */
    /**
     * 加载点赞状态 - 只更新个人点赞状态，不更新总点赞数
     */
    private void loadLikeStatus() {
        if (StaticVar.cookie.isEmpty()) {
            // 用户未登录，设置默认状态
            runOnUiThread(() -> {
                isLiked = false;  // 未登录默认未点赞
                updateLikeUI();
            });
            return;
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("goodsID", goodsID)
                .build();

        String likeStatusUrl = BACKEND_BASE_URL + "/like/status/";

        HttpUtil.sendOkHttpRequest(likeStatusUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    isLiked = false;  // 失败时默认未点赞
                    updateLikeUI();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        boolean success = jsonObject.optBoolean("success", false);

                        if (success) {
                            boolean liked = jsonObject.optBoolean("liked", false);
                            // ❌ 不要从个人状态接口获取点赞数，总点赞数已经来自商品详情
                            // int count = jsonObject.optInt("like_count", likeCount);

                            runOnUiThread(() -> {
                                isLiked = liked;  // ✅ 只更新个人点赞状态
                                updateLikeUI();   // ✅ 更新UI
                            });
                        } else {
                            runOnUiThread(() -> {
                                isLiked = false;
                                updateLikeUI();
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            isLiked = false;
                            updateLikeUI();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        isLiked = false;
                        updateLikeUI();
                    });
                }
            }
        });
    }

    /**
     * 加载关注状态 - 修正版
     */
    private void loadFollowStatus() {
        if (sellerUserId.isEmpty()) {
            return;
        }

        // ========== 修正：参数名改为 targetAccount ==========
        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("targetAccount", sellerUserId)  // 修改参数名
                .build();

        String followStatusUrl = BACKEND_BASE_URL + "/follow/status/";

        HttpUtil.sendOkHttpRequest(followStatusUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 静默失败，不影响主流程
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String responseData = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        boolean success = jsonObject.optBoolean("success", false);

                        if (success) {
                            boolean following = jsonObject.optBoolean("following", false);

                            runOnUiThread(() -> {
                                isFollowing = following;
                                updateFollowUI();
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private String getFinalImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "android.resource://" + getPackageName() + "/" + R.mipmap.ic_launcher;
        }
        if (imagePath.startsWith("http")) {
            return imagePath;
        }
        if (imagePath.startsWith("/upload/")) {
            return BACKEND_BASE_URL + imagePath;
        }
        return BACKEND_BASE_URL + "/" + imagePath;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.good_detail_back:
                finish();
                break;
            case R.id.good_detail_user_message:
                Intent intent = new Intent(good_detail.this, chat.class);
                intent.putExtra("username", username);
                intent.putExtra("friend_name", user_name.getText().toString());
                intent.putExtra("goodsId", goodsID);
                startActivity(intent);
                break;
            case R.id.btn_send_comment:
                sendComment();
                break;
            // 立即购买按钮点击事件 - 现在跳转到支付确认页面
            case R.id.btn_buy_now:
                goToBuySuccess();  // 调用修改后的方法
                break;
            // ========== 新增：关注按钮点击事件 ==========
            case R.id.good_detail_follow_btn:
                handleFollowClick();
                break;
            // ========== 新增：点赞按钮点击事件 ==========
            case R.id.good_detail_like_container:
                handleLikeClick();
                break;
        }
    }

    /**
     * 跳转到支付确认页面（添加购买自己的商品检查）
     */
    private void goToBuySuccess() {
        // 检查用户是否登录
        if (StaticVar.cookie.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查卖家信息
        if (sellerAccount.isEmpty()) {
            Toast.makeText(this, "卖家信息获取失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取当前登录用户的账号
        // 确保StaticVar已经加载了用户信息
        StaticVar.loadUserInfo(this);
        String currentUserAccount = StaticVar.user_name;

        if (currentUserAccount == null || currentUserAccount.isEmpty()) {
            Toast.makeText(this, "用户信息异常，请重新登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 检查当前用户是否是卖家
        if (sellerAccount.equals(currentUserAccount)) {
            Toast.makeText(this, "不能购买自己发布的商品", Toast.LENGTH_SHORT).show();
            return;
        }

        // 跳转到支付确认页面
        Intent intent = new Intent(good_detail.this, PurchaseDetailActivity.class);
        intent.putExtra("goodsID", goodsID);
        intent.putExtra("productName", currentContent);
        intent.putExtra("price", currentPrice);
        intent.putExtra("sellerAccount", sellerAccount);
        intent.putExtra("sellerName", sellerName);
        // 也传递当前用户账号，以便在支付页面进行二次验证
        intent.putExtra("buyerAccount", currentUserAccount);
        startActivity(intent);
    }

    // 原有的内部类保持不变
    public static class Photo {
        private Uri imageUri;
        public Photo(Uri imageUri) { this.imageUri = imageUri; }
        public Uri getImageUri() { return imageUri; }
    }

    public static class Comment {
        private String commentId;
        private String goodsId;
        private String userId;
        private String userName;
        private String content;
        private Date createTime;
        public Comment(String commentId, String goodsId, String userId, String userName, String content, Date createTime) {
            this.commentId = commentId;
            this.goodsId = goodsId;
            this.userId = userId;
            this.userName = userName;
            this.content = content;
            this.createTime = createTime;
        }
        // 可以添加getter方法
        public String getUserName() { return userName; }
        public String getContent() { return content; }
        public Date getCreateTime() { return createTime; }
    }

    public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {
        private List<Photo> photoList;
        public PhotoAdapter(List<Photo> photoList) { this.photoList = photoList; }
        @Override
        public PhotoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.setail_photo_item, parent, false);
            return new PhotoViewHolder(view);
        }
        @Override
        public void onBindViewHolder(PhotoViewHolder holder, int position) {
            // 你的绑定逻辑
            if (position < photoList.size()) {
                Photo photo = photoList.get(position);
                // 使用Picasso加载图片
                Picasso.get()
                        .load(photo.getImageUri())
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .into(holder.imageView);
            }
        }
        @Override
        public int getItemCount() { return photoList.size(); }
        class PhotoViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            public PhotoViewHolder(View itemView) {
                super(itemView);
                // 根据你的布局设置正确的ID
                imageView = itemView.findViewById(R.id.photo_item_photo);
            }
        }
    }

    public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {
        private List<Comment> commentList;
        public CommentAdapter(List<Comment> commentList) { this.commentList = commentList; }
        public void setCommentList(List<Comment> commentList) {
            this.commentList = commentList;
            notifyDataSetChanged();
        }
        @Override
        public CommentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }
        @Override
        public void onBindViewHolder(CommentViewHolder holder, int position) {
            if (position < commentList.size()) {
                Comment comment = commentList.get(position);
                // 绑定评论数据
                holder.userNameTextView.setText(comment.getUserName());
                holder.contentTextView.setText(comment.getContent());

                // 格式化时间
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                String timeStr = sdf.format(comment.getCreateTime());
                holder.timeTextView.setText(timeStr);
            }
        }
        @Override
        public int getItemCount() { return commentList.size(); }
        class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView userNameTextView;
            TextView contentTextView;
            TextView timeTextView;
            public CommentViewHolder(View itemView) {
                super(itemView);
                // 根据你的item_comment布局设置正确的ID
                userNameTextView = itemView.findViewById(R.id.tv_comment_user_name);
                contentTextView = itemView.findViewById(R.id.tv_comment_content);
                timeTextView = itemView.findViewById(R.id.tv_comment_time);
            }
        }
    }
}