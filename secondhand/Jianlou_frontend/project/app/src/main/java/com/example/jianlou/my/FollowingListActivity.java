package com.example.jianlou.my;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FollowingListActivity extends AppCompatActivity {
    private static final String TAG = "FollowingList";
    private static final String BACKEND_BASE_URL = "http://192.168.34.31:8080";

    private RecyclerView recyclerView;
    private TextView emptyView, titleView;
    private FollowingAdapter adapter;
    private List<UserInfo> followingList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_list);

        initView();
        loadFollowingList();
    }

    private void initView() {
        recyclerView = findViewById(R.id.user_list_recycler);
        emptyView = findViewById(R.id.empty_view);
        titleView = findViewById(R.id.title_text);

        titleView.setText("我的关注");

        // 设置返回按钮
        findViewById(R.id.back_button).setOnClickListener(v -> finish());

        // 初始化RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new FollowingAdapter(this, followingList);
        recyclerView.setAdapter(adapter);
    }

    private void loadFollowingList() {
        if (StaticVar.cookie.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String url = BACKEND_BASE_URL + "/follow/following/";
        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .build();

        com.example.jianlou.Internet.HttpUtil.sendOkHttpRequest(url, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(FollowingListActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show();
                    showEmptyView();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "关注列表响应: " + responseData);

                    parseFollowingList(responseData);
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(FollowingListActivity.this, "获取失败: " + response.code(), Toast.LENGTH_SHORT).show();
                        showEmptyView();
                    });
                }
            }
        });
    }

    private void parseFollowingList(String responseData) {
        try {
            JSONObject jsonObject = new JSONObject(responseData);
            boolean success = jsonObject.optBoolean("success", false);

            if (success) {
                JSONArray dataArray = jsonObject.optJSONArray("data");
                followingList.clear();

                if (dataArray != null && dataArray.length() > 0) {
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject userJson = dataArray.getJSONObject(i);
                        UserInfo userInfo = new UserInfo();
                        userInfo.setAccount(userJson.optString("account", ""));
                        userInfo.setUsername(userJson.optString("username", "未知用户"));
                        userInfo.setAvatar(userJson.optString("avatar", ""));
                        userInfo.setFollowing(true); // 关注列表中的人都是已关注的

                        followingList.add(userInfo);
                    }

                    runOnUiThread(() -> {
                        adapter.setUserList(followingList);
                        adapter.notifyDataSetChanged();
                        hideEmptyView();
                    });
                } else {
                    runOnUiThread(this::showEmptyView);
                }
            } else {
                String message = jsonObject.optString("message", "获取关注列表失败");
                runOnUiThread(() -> {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    showEmptyView();
                });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(this, "数据解析失败", Toast.LENGTH_SHORT).show();
                showEmptyView();
            });
        }
    }

    private void showEmptyView() {
        emptyView.setVisibility(View.VISIBLE);
        emptyView.setText("暂无关注");
        recyclerView.setVisibility(View.GONE);
    }

    private void hideEmptyView() {
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    // 用户信息类
    public static class UserInfo {
        private String account;
        private String username;
        private String avatar;
        private boolean isFollowing;

        public String getAccount() { return account; }
        public void setAccount(String account) { this.account = account; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }

        public boolean isFollowing() { return isFollowing; }
        public void setFollowing(boolean following) { isFollowing = following; }
    }

    // 关注列表适配器
    public class FollowingAdapter extends RecyclerView.Adapter<FollowingAdapter.ViewHolder> {
        private Context context;
        private List<UserInfo> userList;

        public FollowingAdapter(Context context, List<UserInfo> userList) {
            this.context = context;
            this.userList = userList;
        }

        public void setUserList(List<UserInfo> userList) {
            this.userList = userList;
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UserInfo user = userList.get(position);

            holder.userName.setText(user.getUsername());
            holder.userAccount.setText("账号: " + user.getAccount());

            // 设置关注按钮状态
            if (user.isFollowing()) {
                holder.actionButton.setText("已关注");
                holder.actionButton.setBackgroundResource(R.drawable.btn_followed_bg);
                holder.actionButton.setTextColor(getResources().getColor(R.color.color_999999));
            } else {
                holder.actionButton.setText("关注");
                holder.actionButton.setBackgroundResource(R.drawable.btn_follow_bg);
                holder.actionButton.setTextColor(getResources().getColor(R.color.white));
            }

            // 关注/取消关注点击
            holder.actionButton.setOnClickListener(v -> {
                toggleFollow(user, holder, position);
            });

            // 点击整个item跳转到用户主页（可选）
            holder.itemView.setOnClickListener(v -> {
                // 可以跳转到用户主页
                Toast.makeText(context, "查看" + user.getUsername() + "的主页", Toast.LENGTH_SHORT).show();
            });
        }

        private void toggleFollow(UserInfo user, ViewHolder holder, int position) {
            if (StaticVar.cookie.isEmpty()) {
                Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }

            String url = BACKEND_BASE_URL + "/follow/toggle/";
            RequestBody requestBody = new FormBody.Builder()
                    .add("cookie", StaticVar.cookie)
                    .add("targetAccount", user.getAccount())
                    .build();

            com.example.jianlou.Internet.HttpUtil.sendOkHttpRequest(url, requestBody, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(context, "网络请求失败", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.code() == 200 && response.body() != null) {
                        String responseData = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(responseData);
                            boolean success = jsonObject.optBoolean("success", false);

                            runOnUiThread(() -> {
                                if (success) {
                                    boolean following = jsonObject.optBoolean("following", false);
                                    user.setFollowing(following);

                                    if (following) {
                                        holder.actionButton.setText("已关注");
                                        holder.actionButton.setBackgroundResource(R.drawable.btn_followed_bg);
                                        holder.actionButton.setTextColor(getResources().getColor(R.color.color_999999));
                                        Toast.makeText(context, "关注成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        holder.actionButton.setText("关注");
                                        holder.actionButton.setBackgroundResource(R.drawable.btn_follow_bg);
                                        holder.actionButton.setTextColor(getResources().getColor(R.color.white));
                                        Toast.makeText(context, "已取消关注", Toast.LENGTH_SHORT).show();

                                        // 如果是在关注列表页面，取消关注后从列表中移除
                                        followingList.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, followingList.size());

                                        if (followingList.isEmpty()) {
                                            showEmptyView();
                                        }
                                    }
                                } else {
                                    String message = jsonObject.optString("message", "操作失败");
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                            runOnUiThread(() ->
                                    Toast.makeText(context, "数据解析失败", Toast.LENGTH_SHORT).show());
                        }
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return userList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            com.example.jianlou.view.CircleImageView userAvatar;
            TextView userName, userAccount;
            android.widget.Button actionButton;

            ViewHolder(View itemView) {
                super(itemView);
                userAvatar = itemView.findViewById(R.id.user_avatar);
                userName = itemView.findViewById(R.id.user_name);
                userAccount = itemView.findViewById(R.id.user_account);
                actionButton = itemView.findViewById(R.id.action_button);
            }
        }
    }
}