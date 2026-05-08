package com.example.jianlou.shequ;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.index.Photo;
import com.example.jianlou.index.PhotoAdapter;
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

public class PostDetailActivity extends AppCompatActivity implements View.OnClickListener {
    private String postId; // 帖子ID
    ImageView back, head;
    TextView tvPostTitle, tvPostType, tvPostCommunity, tvUserName, tvContent, tvTime;

    RecyclerView recyclerView;
    private ProgressBar progressBar;
    // 复用index包的Photo类，全局图片列表
    private final List<Photo> photoList = new ArrayList<>();
    private static final String BACKEND_BASE_URL = "http://192.168.93.1:8080";
    // 复用index包的PhotoAdapter
    private PhotoAdapter photoAdapter;
    private String username; // 卖家/作者账号

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);
        // 获取传递的帖子ID
        postId = getIntent().getStringExtra("postId");
        // 加载用户信息
        StaticVar.loadUserInfo(this);
        initView();
        // 加载帖子详情
        initPostDetail();
    }

    /**
     * 初始化视图控件（参照good_detail逻辑）
     */
    private void initView() {
        // 绑定帖子详情控件
        progressBar = findViewById(R.id.post_detail_bar);
        back = findViewById(R.id.post_detail_back);
        head = findViewById(R.id.post_detail_photo);
        tvUserName = findViewById(R.id.post_detail_user_name);
        tvPostTitle = findViewById(R.id.post_detail_title);
        tvPostType = findViewById(R.id.post_detail_type);
        tvPostCommunity = findViewById(R.id.post_detail_community);
        tvContent = findViewById(R.id.post_detail_content);
        tvTime = findViewById(R.id.post_detail_time);
        recyclerView = findViewById(R.id.post_detail_recycle);



        // 绑定点击事件
        back.setOnClickListener(this);

        // 初始化图片RecyclerView（完全参照good_detail）
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        // 复用index包的PhotoAdapter
        photoAdapter = new PhotoAdapter(photoList);
        recyclerView.setAdapter(photoAdapter);


    }

    /**
     * 加载帖子详情数据（参照good_detail的initPhoto逻辑）
     */
    private void initPostDetail() {
        photoList.clear();
        photoAdapter.notifyDataSetChanged();
        progressBar.setVisibility(View.VISIBLE);

        // 构建帖子详情请求参数
        RequestBody requestBody = new FormBody.Builder()
                .add("postId", postId)
                .build();

        // 帖子详情接口地址
        String postDetailUrl = BACKEND_BASE_URL + "/post/detail/";

        HttpUtil.sendOkHttpRequest(postDetailUrl, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(PostDetailActivity.this, "请求失败，请检查网络", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handlePostResponse(response);
            }
        });
    }

    /**
     * 处理帖子详情响应数据（参照good_detail的handleResponse逻辑）
     */
    private void handlePostResponse(Response response) throws IOException {
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));

        if (response.code() != 200) {
            runOnUiThread(() ->
                    Toast.makeText(PostDetailActivity.this, "服务器故障：" + response.code(), Toast.LENGTH_SHORT).show()
            );
            return;
        }

        String responseData = response.body() == null ? "" : response.body().string();
        if ("failed".equals(responseData) || responseData.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(PostDetailActivity.this, "服务器返回异常数据", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(responseData);
            // 解析帖子基础信息
            String code = jsonObject.optString("code", "failed");
            if (!"success".equals(code)) {
                runOnUiThread(() ->
                        Toast.makeText(PostDetailActivity.this, jsonObject.optString("msg", "帖子不存在"), Toast.LENGTH_SHORT).show()
                );
                return;
            }

            String postTitle = jsonObject.optString("postTitle", "无标题");
            String postContent = jsonObject.optString("postContent", "暂无内容");
            String postType = jsonObject.optString("postType", "未分类");
            String postCommunity = jsonObject.optString("postCommunity", "未归属社区");
            String userName = jsonObject.optString("userName", "未知用户");
            username = jsonObject.optString("username", ""); // 保存作者账号
            String userHead = jsonObject.optString("userHead", "");
            String timeStr = jsonObject.optString("postTime", "");

            // 解析帖子图片（完全参照good_detail逻辑）
            JSONArray imagesArray = jsonObject.optJSONArray("imageList");
            photoList.clear();
            if (imagesArray != null && imagesArray.length() > 0) {
                for (int i = 0; i < imagesArray.length(); i++) {
                    JSONObject imgObj = imagesArray.getJSONObject(i);
                    String imageUrl = imgObj.optString("imageUrl", "");
                    String finalImgUrl = getFinalImageUrl(imageUrl);
                    // 复用index包Photo类，通过Uri创建对象
                    photoList.add(new Photo(Uri.parse(finalImgUrl)));
                }
            }

            // 更新UI
            runOnUiThread(() -> {
                tvPostTitle.setText(postTitle);
                tvPostType.setText(postType);
                tvPostCommunity.setText("所属社区：" + postCommunity);
                tvUserName.setText(userName);
                tvContent.setText(postContent);

                // 时间格式化（参照good_detail）
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                Date createTime;
                try {
                    createTime = isoFormat.parse(timeStr);
                    String formattedTime = targetFormat.format(createTime);
                    tvTime.setText("发布于：" + formattedTime);
                } catch (Exception e) {
                    createTime = new Date();
                    tvTime.setText("发布于：" + targetFormat.format(createTime));
                }

                // 加载用户头像（参照good_detail，使用CircleTransform）
                if (TextUtils.isEmpty(userHead)) {
                    Picasso.get()
                            .load(R.mipmap.cat)
                            .transform(new CircleTransform())
                            .into(head);
                } else {
                    String finalHeadUrl = getFinalImageUrl(userHead);
                    Picasso.get()
                            .load(finalHeadUrl)
                            .placeholder(R.mipmap.cat)
                            .error(R.mipmap.cat)
                            .transform(new CircleTransform())
                            .into(head);
                }

                // 刷新图片列表（参照good_detail）
                photoAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(photoAdapter);
            });

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(PostDetailActivity.this, "数据解析失败：" + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }



    /**
     * 处理图片URL（完全复用good_detail的逻辑，确保一致性）
     */
    private String getFinalImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "android.resource://" + getPackageName() + "/" + R.mipmap.ic_launcher;
        }
        if (imagePath.startsWith("http")) {
            return imagePath;
        }
        if (imagePath.startsWith("/upload/post/")) {
            return BACKEND_BASE_URL + imagePath;
        }
        return BACKEND_BASE_URL + "/" + imagePath;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.post_detail_back:
                finish();
                break;
        }
    }



}