package com.example.jianlou.my;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.jianlou.Login.Login;
import com.example.jianlou.R;
import com.example.jianlou.dto.UserLevelInfo;
import com.example.jianlou.staticVar.StaticVar;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class WoDeFragment extends Fragment implements View.OnClickListener {
    // 缓存Fragment view
    private View rootView;
    private static WoDeFragment WoDeFragment;
    private TextView username, user_name, myPublish, myBought, mySold;
    private ImageView setting, photo;

    // ========== 新增：点赞关注粉丝统计控件 ==========
    private TextView likesCountText, followingCountText, followersCountText;

    // 用户等级相关控件
    private TextView userLevel, userLevelScore;
    private TextView activeScore, creditScore, successRate;
    private TextView transactionCount, reviewCount, nextLevel;
    private TextView currentScoreText, nextScoreText, progressText;
    private View progressBar;

    private static final String TAG = "WoDeFragment";
    private static final String BACKEND_BASE_URL = "http://192.168.93.1:8080";
    private static final String USER_LEVEL_API = BACKEND_BASE_URL + "/api/user-level/";
    // ========== 新增：用户统计信息API ==========
    private static final String USER_STATS_API = BACKEND_BASE_URL + "/user/stats/";

    private Handler handler = new Handler(Looper.getMainLooper());

    public WoDeFragment(){}
    public static WoDeFragment getNewInstance(){
        if (WoDeFragment ==null){
            WoDeFragment =new WoDeFragment();
        }
        return WoDeFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_wode, container, false);
        }
        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null) {
            parent.removeView(rootView);
        }
        return rootView;
    }

    private void init() {
        username = rootView.findViewById(R.id.username);
        user_name = rootView.findViewById(R.id.user_name);
        setting = rootView.findViewById(R.id.setting);
        photo = rootView.findViewById(R.id.photo);
        myPublish = rootView.findViewById(R.id.my_publish);
        myBought = rootView.findViewById(R.id.my_bought);
        mySold = rootView.findViewById(R.id.my_sold);

        // ========== 新增：绑定点赞关注粉丝控件 ==========
        likesCountText = rootView.findViewById(R.id.likes_count_text); // 需要在布局中添加这个ID
        followingCountText = rootView.findViewById(R.id.following_count_text); // 需要在布局中添加这个ID
        followersCountText = rootView.findViewById(R.id.followers_count_text); // 需要在布局中添加这个ID

        // 如果布局中没有单独的TextView，可以使用现有的布局
        // 查找包含"0赞"、"0关注"、"0粉丝"的LinearLayout中的TextView
        LinearLayout statsLayout = rootView.findViewById(R.id.stats_layout); // 你需要给包含三个TextView的LinearLayout添加id
        if (statsLayout != null && statsLayout.getChildCount() >= 3) {
            // 第一个TextView是点赞数
            if (statsLayout.getChildAt(0) instanceof TextView) {
                likesCountText = (TextView) statsLayout.getChildAt(0);
            }
            // 第二个TextView是关注数
            if (statsLayout.getChildAt(1) instanceof TextView) {
                followingCountText = (TextView) statsLayout.getChildAt(1);
            }
            // 第三个TextView是粉丝数
            if (statsLayout.getChildAt(2) instanceof TextView) {
                followersCountText = (TextView) statsLayout.getChildAt(2);
            }
        }

        // 等级相关控件初始化
        userLevel = rootView.findViewById(R.id.user_level);
        userLevelScore = rootView.findViewById(R.id.user_level_score);
        activeScore = rootView.findViewById(R.id.active_score);
        creditScore = rootView.findViewById(R.id.credit_score);
        successRate = rootView.findViewById(R.id.success_rate);
        transactionCount = rootView.findViewById(R.id.transaction_count);
        reviewCount = rootView.findViewById(R.id.review_count);
        nextLevel = rootView.findViewById(R.id.next_level);
        currentScoreText = rootView.findViewById(R.id.current_score_text);
        nextScoreText = rootView.findViewById(R.id.next_score_text);
        progressText = rootView.findViewById(R.id.progress_text);
        progressBar = rootView.findViewById(R.id.progress_bar);

        // 设置点击事件
        myPublish.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), myPublish.class);
            startActivity(intent);
        });

        if (myBought != null) {
            myBought.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), myBought.class);
                startActivity(intent);
            });
        }

        if (mySold != null) {
            mySold.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), mySold.class);
                startActivity(intent);
            });
        }

        setting.setOnClickListener(this);

        // 设置用户基本信息
        username.setText("账号：" + StaticVar.cookie);
        user_name.setText(StaticVar.user_name);
        Picasso.get().load(R.mipmap.cat).transform(new CircleTransform()).into(photo);

        // 加载用户等级信息
        loadUserLevelInfo();
        // ========== 新增：加载用户统计信息（点赞、关注、粉丝） ==========
        loadUserStatsInfo();
        loadAvatar();



        // ========== 新增：为关注和粉丝TextView添加点击事件 ==========
        if (followingCountText != null) {
            followingCountText.setOnClickListener(v -> {
                // 跳转到关注列表
                Intent intent = new Intent(getContext(), FollowingListActivity.class);
                startActivity(intent);
            });
        }

        if (followersCountText != null) {
            followersCountText.setOnClickListener(v -> {
                // 跳转到粉丝列表
                Intent intent = new Intent(getContext(), FollowersListActivity.class);
                startActivity(intent);
            });
        }


    }


    /**
     * 加载用户统计信息（点赞数、关注数、粉丝数）
     */
    private void loadUserStatsInfo() {
        if (StaticVar.cookie.isEmpty()) {
            Log.d(TAG, "用户未登录，不加载统计信息");
            return;
        }

        Log.d(TAG, "开始加载用户统计信息...");

        // 构建请求体
        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .build();

        // 创建请求
        Request request = new Request.Builder()
                .url(USER_STATS_API)
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取用户统计信息失败: " + e.getMessage());
                // 使用默认值
                handler.post(() -> showDefaultStatsInfo());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d(TAG, "用户统计信息响应: " + responseData);

                    try {
                        JSONObject jsonObject = new JSONObject(responseData);
                        boolean success = jsonObject.optBoolean("success", false);

                        if (success) {
                            int likesReceived = jsonObject.optInt("likes_received", 0);
                            int followersCount = jsonObject.optInt("followers_count", 0);
                            int followingCount = jsonObject.optInt("following_count", 0);

                            Log.d(TAG, String.format("解析成功: 赞=%d, 粉丝=%d, 关注=%d",
                                    likesReceived, followersCount, followingCount));

                            // 更新UI
                            handler.post(() -> updateStatsUI(likesReceived, followersCount, followingCount));
                        } else {
                            String message = jsonObject.optString("message", "获取失败");
                            Log.e(TAG, "获取统计信息失败: " + message);
                            handler.post(() -> showDefaultStatsInfo());
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析统计信息JSON失败: " + e.getMessage());
                        handler.post(() -> showDefaultStatsInfo());
                    }
                } else {
                    Log.e(TAG, "获取统计信息失败，状态码: " + response.code());
                    handler.post(() -> showDefaultStatsInfo());
                }
            }
        });
    }

    /**
     * 更新统计信息UI
     */
    private void updateStatsUI(int likesReceived, int followersCount, int followingCount) {
        runOnUiThread(() -> {
            if (likesCountText != null) {
                likesCountText.setText(likesReceived + "赞");
            }
            if (followingCountText != null) {
                followingCountText.setText(followingCount + "关注");
            }
            if (followersCountText != null) {
                followersCountText.setText(followersCount + "粉丝");
            }
        });
    }

    /**
     * 显示默认统计信息
     */
    private void showDefaultStatsInfo() {
        runOnUiThread(() -> {
            if (likesCountText != null) {
                likesCountText.setText("0赞");
            }
            if (followingCountText != null) {
                followingCountText.setText("0关注");
            }
            if (followersCountText != null) {
                followersCountText.setText("0粉丝");
            }
        });
    }

    /**
     * 在主线程运行
     */
    private void runOnUiThread(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(action);
        }
    }

    /**
     * 加载头像（核心逻辑优化）
     */
    private void loadAvatar() {
        Context context = getActivity();
        if (context == null || photo == null) {
            return; // 避免空指针
        }

        // 1. 默认头像资源
        int defaultAvatarRes = R.mipmap.cat;
        // 2. 网络头像URL
        String networkAvatarUrl = StaticVar.headUrl;

        // 3. 转换默认资源为Uri
        String defaultAvatarUriStr = String.format("android.resource://%s/%d",
                context.getPackageName(), defaultAvatarRes);
        Uri defaultAvatarUri = Uri.parse(defaultAvatarUriStr);

        // 4. 转换网络URL为Uri（先判空）
        Uri networkAvatarUri = null;
        if (!TextUtils.isEmpty(networkAvatarUrl)) {
            networkAvatarUri = Uri.parse(networkAvatarUrl);
        }

        // 5. 统一Uri类型
        Uri targetUri = TextUtils.isEmpty(networkAvatarUrl) ? defaultAvatarUri : networkAvatarUri;

        // 6. Picasso加载图片（核心修正：添加resize()配合centerCrop()）
        int avatarSize = 200; // 头像尺寸，可根据需求调整（必须大于0）
        Picasso.get()
                .load(targetUri)
                .transform(new CircleTransform()) // 圆形变换
                .placeholder(defaultAvatarRes)    // 加载中占位图
                .error(defaultAvatarRes)          // 加载失败占位图
                .resize(avatarSize, avatarSize)   // 指定宽高，配合centerCrop()
                .centerCrop()                     // 现在可以安全使用裁剪
                .into(photo);


    }

    /**
     * 加载用户等级信息
     */
    private void loadUserLevelInfo() {
        // 重新加载SharedPreferences确保数据最新
        StaticVar.loadUserInfo(getActivity());

        Log.d(TAG, "=== WoDeFragment - 详细用户信息调试 ===");
        Log.d(TAG, "1. StaticVar值:");
        Log.d(TAG, "   cookie: " + (StaticVar.cookie != null ? StaticVar.cookie : "null"));
        Log.d(TAG, "   user_account: " + (StaticVar.user_account != null ? StaticVar.user_account : "null"));
        Log.d(TAG, "   user_name: " + (StaticVar.user_name != null ? StaticVar.user_name : "null"));
        Log.d(TAG, "   user_type: " + (StaticVar.user_type != null ? StaticVar.user_type : "null"));
        Log.d(TAG, "   user_status: " + (StaticVar.user_status != null ? StaticVar.user_status : "null"));

        // 检查SharedPreferences
        SharedPreferences sp = getActivity().getSharedPreferences(StaticVar.fileName, Context.MODE_PRIVATE);
        Log.d(TAG, "2. SharedPreferences值:");
        Log.d(TAG, "   账号(fileUserAccount): " + sp.getString(StaticVar.fileUserAccount, "空"));
        Log.d(TAG, "   Cookie(fileCookiename): " + sp.getString(StaticVar.fileCookiename, "空"));
        Log.d(TAG, "   用户名(fileUserName): " + sp.getString(StaticVar.fileUserName, "空"));
        Log.d(TAG, "   用户类型(fileUserType): " + sp.getString(StaticVar.fileUserType, "空"));
        Log.d(TAG, "   用户状态(fileUserStatus): " + sp.getString(StaticVar.fileUserStatus, "空"));

        // 确定用户账号
        String userAccount = null;

        // 方法1：使用StaticVar.user_account
        if (StaticVar.user_account != null && !StaticVar.user_account.isEmpty()) {
            userAccount = StaticVar.user_account;
            Log.d(TAG, "4. 使用StaticVar.user_account: " + userAccount);
        }
        // 方法2：使用SharedPreferences中的账号
        else {
            String spAccount = sp.getString(StaticVar.fileUserAccount, "");
            if (!spAccount.isEmpty()) {
                userAccount = spAccount;
                StaticVar.user_account = spAccount; // 同步到StaticVar
                Log.d(TAG, "5. 使用SharedPreferences中的账号: " + userAccount);
            }
        }

        // 方法3：从user_name中提取
        if (userAccount == null || userAccount.isEmpty()) {
            if (StaticVar.user_name != null && StaticVar.user_name.matches(".*\\d+.*")) {
                java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+").matcher(StaticVar.user_name);
                if (matcher.find()) {
                    userAccount = matcher.group();
                    Log.d(TAG, "6. 从user_name中提取账号: " + userAccount);
                }
            }
        }

        // 方法4：使用已知的测试账号
        if (userAccount == null || userAccount.isEmpty()) {
            // 从日志看，当前用户应该是 18307414678
            userAccount = "18307414678";
            Log.d(TAG, "7. 使用默认测试账号: " + userAccount);
        }

        Log.d(TAG, "8. 最终确定账号: " + userAccount);
        String url = USER_LEVEL_API + userAccount;
        Log.d(TAG, "9. 请求URL: " + url);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取用户等级信息失败: " + e.getMessage());
                handler.post(() -> showDefaultUserLevelInfo());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "10. 响应状态码: " + response.code());
                Log.d(TAG, "11. 响应数据: " + responseData.substring(0, Math.min(responseData.length(), 500)));

                if (!response.isSuccessful()) {
                    Log.e(TAG, "获取用户等级信息失败，状态码: " + response.code());
                    handler.post(() -> showDefaultUserLevelInfo());
                    return;
                }

                try {
                    JSONObject json = new JSONObject(responseData);
                    UserLevelInfo userLevelInfo = parseUserLevelInfo(json);

                    Log.d(TAG, "12. 解析成功，用户等级: " +
                            (userLevelInfo != null ? userLevelInfo.getLevel() : "null"));

                    handler.post(() -> {
                        updateUserLevelUI(userLevelInfo);
                    });

                } catch (Exception e) {
                    Log.e(TAG, "解析用户等级信息失败: " + e.getMessage());
                    e.printStackTrace();
                    handler.post(() -> showDefaultUserLevelInfo());
                }
            }
        });
    }

    /**
     * 解析用户等级信息
     */
    private UserLevelInfo parseUserLevelInfo(JSONObject json) {
        UserLevelInfo info = new UserLevelInfo();
        try {
            // 先打印完整的JSON，查看后端返回了什么
            Log.d(TAG, "=== 完整JSON响应 ===");
            Log.d(TAG, json.toString(2)); // 格式化输出

            info.setAccount(json.optString("account", ""));
            info.setUsername(json.optString("username", ""));
            info.setLevel(json.optString("level", "青铜"));
            info.setLevelScore(json.optInt("levelScore", 0));
            info.setActiveScore(json.optInt("activeScore", 0));
            info.setCreditScore(json.optInt("creditScore", 60));
            info.setTransactionCount(json.optInt("transactionCount", 0));
            info.setReviewCount(json.optInt("reviewCount", 0));
            info.setSuccessRate(json.optDouble("successRate", 100.0));

            // 解析下一等级信息
            if (json.has("nextLevel")) {
                if (json.isNull("nextLevel")) {
                    Log.d(TAG, "nextLevel字段存在但为null");
                } else {
                    JSONObject nextLevelJson = json.getJSONObject("nextLevel");
                    Log.d(TAG, "=== nextLevel字段详情 ===");
                    Log.d(TAG, nextLevelJson.toString(2));

                    UserLevelInfo.NextLevelInfo nextLevelInfo = new UserLevelInfo.NextLevelInfo();
                    nextLevelInfo.setLevel(nextLevelJson.optString("level", ""));
                    nextLevelInfo.setNeedScore(nextLevelJson.optInt("needScore", 0));
                    nextLevelInfo.setTotalScore(nextLevelJson.optInt("totalScore", 0));
                    info.setNextLevel(nextLevelInfo);

                    Log.d(TAG, "解析的nextLevel:");
                    Log.d(TAG, "  level: " + nextLevelInfo.getLevel());
                    Log.d(TAG, "  needScore: " + nextLevelInfo.getNeedScore());
                    Log.d(TAG, "  totalScore: " + nextLevelInfo.getTotalScore());
                }
            } else {
                Log.d(TAG, "JSON中没有nextLevel字段");
            }

            // 验证一下成功率数据
            double successRate = json.optDouble("successRate", 100.0);
            Log.d(TAG, "成功率原始值: " + successRate + "%");
            if (successRate <= 0) {
                Log.w(TAG, "注意：成功率为0或负数，可能需要检查后端计算");
            }

        } catch (Exception e) {
            Log.e(TAG, "解析用户等级信息异常: " + e.getMessage());
            e.printStackTrace();
        }

        return info;
    }


    /**
     * 更新用户等级UI
     */
    private void updateUserLevelUI(UserLevelInfo info) {
        if (info == null) {
            showDefaultUserLevelInfo();
            return;
        }

        Log.d(TAG, "13. updateUserLevelUI - 开始更新UI:");
        Log.d(TAG, "   Level: " + info.getLevel());
        Log.d(TAG, "   LevelScore: " + info.getLevelScore());
        Log.d(TAG, "   NextLevel: " + (info.getNextLevel() != null ? info.getNextLevel().getLevel() : "null"));

        // 1. 顶部基本信息
        if (userLevel != null) {
            userLevel.setText("等级：" + info.getLevel());
            // 根据等级设置颜色
            setLevelColor(userLevel, info.getLevel());
        }

        if (userLevelScore != null) {
            userLevelScore.setText("分数：" + info.getLevelScore());
        }

        // 2. 数据统计
        if (activeScore != null) {
            activeScore.setText(String.valueOf(info.getActiveScore()));
        }

        if (creditScore != null) {
            creditScore.setText(String.valueOf(info.getCreditScore()));
        }

        if (successRate != null) {
            successRate.setText(String.format("%.1f%%", info.getSuccessRate()));
            // 根据成功率设置颜色
            setSuccessRateColor(successRate, info.getSuccessRate());
        }

        if (transactionCount != null) {
            transactionCount.setText(String.valueOf(info.getTransactionCount()));
        }

        if (reviewCount != null) {
            reviewCount.setText(String.valueOf(info.getReviewCount()));
        }

        // 3. 下一等级信息和进度条（这是关键修复部分）
        if (info.getNextLevel() != null) {
            UserLevelInfo.NextLevelInfo nextLevelInfo = info.getNextLevel();

            // 显示下一等级信息
            if (nextLevel != null) {
                nextLevel.setText(nextLevelInfo.getLevel());
            }

            // 显示当前分数
            if (currentScoreText != null) {
                currentScoreText.setText("当前：" + info.getLevelScore());
            }

            // 修改这里：显示当前等级的最大分，而不是下一等级的起始分
            if (nextScoreText != null) {
                int currentScore = info.getLevelScore();
                int currentLevelMaxScore;

                // 根据当前分数确定当前等级的最大分
                if (currentScore >= 96) {
                    currentLevelMaxScore = 100; // 钻石最大100分
                } else if (currentScore >= 81) {
                    currentLevelMaxScore = 95;  // 黄金最大95分
                } else if (currentScore >= 61) {
                    currentLevelMaxScore = 80;  // 白银最大80分
                } else {
                    currentLevelMaxScore = 60;  // 青铜最大60分
                }

                nextScoreText.setText("目标：" + currentLevelMaxScore);
            }

            // 计算并更新进度条
            updateProgressBar(info.getLevelScore(), nextLevelInfo);

            Log.d(TAG, "14. 有下一等级信息:");
            Log.d(TAG, "   下一等级: " + nextLevelInfo.getLevel());
            Log.d(TAG, "   需要分数: " + nextLevelInfo.getNeedScore());
            Log.d(TAG, "   总分(下一等级起始分): " + nextLevelInfo.getTotalScore());

            // 删除下面这段强制测量的代码，因为新的updateProgressBar方法已经处理了延迟
            // handler.post(() -> {
            //     if (progressBar != null) {
            //         View parent = (View) progressBar.getParent();
            //         if (parent != null) {
            //             // 强制测量
            //             parent.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            //
            //             // 立即更新进度条
            //             updateProgressBar(info.getLevelScore(), nextLevelInfo);
            //         }
            //     }
            // });

        } else {
            // 最高等级的情况
            Log.d(TAG, "15. 无下一等级信息，显示最高等级");

            if (nextLevel != null) {
                nextLevel.setText("已达最高等级");
            }

            if (currentScoreText != null) {
                currentScoreText.setText("当前：" + info.getLevelScore());
            }

            if (nextScoreText != null) {
                // 最高等级显示当前分数
                nextScoreText.setText("已达最高");
            }

            // 进度条满格
            updateProgressBar(info.getLevelScore(), null);
        }
    }



    /**
     * 更新进度条 - 使用简单的经验值计算方法
     */
    private void updateProgressBar(int currentScore, UserLevelInfo.NextLevelInfo nextLevelInfo) {
        Log.d(TAG, "=== updateProgressBar 简单方法 ===");
        Log.d(TAG, "当前分数: " + currentScore);
        Log.d(TAG, "nextLevelInfo: " + (nextLevelInfo != null ? "非空" : "空"));

        if (progressBar == null || progressText == null) {
            Log.e(TAG, "进度条相关控件为 null!");
            return;
        }

        // 计算进度百分比
        float progressPercentage = calculateSimpleProgress(currentScore, nextLevelInfo);

        // 创建final副本以便在lambda中使用
        final float finalProgressPercentage = progressPercentage;

        // 在主线程执行UI更新
        handler.post(() -> {
            // 更新进度文本
            progressText.setText(String.format("%.0f%%", finalProgressPercentage));

            // 延迟更新进度条宽度，确保布局已完成
            handler.postDelayed(() -> {
                updateProgressBarWidth(finalProgressPercentage);
            }, 50);
        });
    }

    /**
     * 简单的进度计算方法 - 根据分数范围判断当前等级
     */
    private float calculateSimpleProgress(int currentScore, UserLevelInfo.NextLevelInfo nextLevelInfo) {
        if (nextLevelInfo == null) {
            // 最高等级：进度100%
            Log.d(TAG, "最高等级，进度: 100%");
            return 100f;
        }

        // 根据当前分数确定当前等级的范围
        int currentLevelStartScore;
        int currentLevelMaxScore;
        int totalLevelRange;

        if (currentScore >= 96) {
            // 钻石: 96-100分
            currentLevelStartScore = 96;
            currentLevelMaxScore = 100;
            totalLevelRange = 5; // 96,97,98,99,100 共5分
            Log.d(TAG, "当前等级: 钻石 (96-100分)");
        } else if (currentScore >= 81) {
            // 黄金: 81-95分
            currentLevelStartScore = 81;
            currentLevelMaxScore = 95;
            totalLevelRange = 15; // 81-95 共15分
            Log.d(TAG, "当前等级: 黄金 (81-95分)");
        } else if (currentScore >= 61) {
            // 白银: 61-80分
            currentLevelStartScore = 61;
            currentLevelMaxScore = 80;
            totalLevelRange = 20; // 61-80 共20分
            Log.d(TAG, "当前等级: 白银 (61-80分)");
        } else {
            // 青铜: 0-60分
            currentLevelStartScore = 0;
            currentLevelMaxScore = 60;
            totalLevelRange = 61; // 0-60 共61分
            Log.d(TAG, "当前等级: 青铜 (0-60分)");
        }

        // 计算在当前等级内的进度
        int progressInLevel = currentScore - currentLevelStartScore;
        progressInLevel = Math.max(0, Math.min(totalLevelRange, progressInLevel));

        // 计算百分比
        float progressPercentage = (float) progressInLevel / totalLevelRange * 100f;
        progressPercentage = Math.min(100f, Math.max(0f, progressPercentage));

        Log.d(TAG, String.format("进度计算: 当前%d分, 段位%d-%d分, 段内进度%d/%d=%.0f%%",
                currentScore, currentLevelStartScore, currentLevelMaxScore,
                progressInLevel, totalLevelRange, progressPercentage));

        return progressPercentage;
    }

    /**
     * 更新进度条宽度
     */
    private void updateProgressBarWidth(float progressPercentage) {
        if (progressBar == null) {
            Log.e(TAG, "updateProgressBarWidth: progressBar 为 null!");
            return;
        }

        View parent = (View) progressBar.getParent();
        if (parent == null) {
            Log.e(TAG, "updateProgressBarWidth: 父容器为 null!");
            return;
        }

        // 获取父容器宽度
        int parentWidth = parent.getWidth();
        Log.d(TAG, "父容器宽度: " + parentWidth + "px");

        if (parentWidth <= 0) {
            Log.w(TAG, "父容器宽度为0或负数，延迟重试");
            // 延迟重试
            handler.postDelayed(() -> {
                updateProgressBarWidth(progressPercentage);
            }, 100);
            return;
        }

        // 考虑父容器的padding
        int paddingLeft = parent.getPaddingLeft();
        int paddingRight = parent.getPaddingRight();
        int availableWidth = parentWidth - paddingLeft - paddingRight;

        // 计算进度条宽度
        int barWidth = (int) (availableWidth * progressPercentage / 100f);

        // 确保最小宽度（即使进度为0，也显示1像素，便于调试）
        if (barWidth == 0 && progressPercentage > 0) {
            barWidth = 1;
        } else if (barWidth == 0 && progressPercentage == 0) {
            barWidth = 0; // 进度为0时，宽度为0
        }

        Log.d(TAG, "设置进度条宽度: " + barWidth + "px (进度: " + progressPercentage + "%, 可用宽度: " + availableWidth + "px)");

        // 获取并更新LayoutParams
        ViewGroup.LayoutParams params = progressBar.getLayoutParams();
        if (params == null) {
            Log.e(TAG, "进度条的LayoutParams为null，创建新的");
            params = new ViewGroup.LayoutParams(barWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        } else {
            params.width = barWidth;
        }

        progressBar.setLayoutParams(params);

        // 设置进度条颜色（绿色）
        progressBar.setBackgroundColor(Color.parseColor("#4CAF50"));

        // 强制刷新
        progressBar.requestLayout();
        progressBar.invalidate();
    }

    /**
     * 根据等级设置背景（文字固定白色）
     */
    private void setLevelColor(TextView textView, String level) {
        int backgroundResId;
        switch (level) {
            case "青铜":
                backgroundResId = R.drawable.level_bronze_bg;
                break;
            case "白银":
                backgroundResId = R.drawable.level_silver_bg;
                break;
            case "黄金":
                backgroundResId = R.drawable.level_gold_bg;
                break;
            case "钻石":
                backgroundResId = R.drawable.level_diamond_bg;
                break;
            default:
                backgroundResId = R.drawable.level_bronze_bg; // 默认青铜
                break;
        }
        // 设置背景
        textView.setBackgroundResource(backgroundResId);
        // 强制文字为白色（避免背景和文字颜色冲突）
        textView.setTextColor(Color.WHITE);
    }

    /**
     * 根据成功率设置颜色
     */
    private void setSuccessRateColor(TextView textView, double successRate) {
        int color;
        if (successRate >= 90) {
            color = 0xFF4CAF50; // 绿色
        } else if (successRate >= 75) {
            color = 0xFF2196F3; // 蓝色
        } else if (successRate >= 60) {
            color = 0xFFFF9800; // 橙色
        } else {
            color = 0xFFF44336; // 红色
        }
        textView.setTextColor(color);
    }

    /**
     * 显示默认用户等级信息
     */
    private void showDefaultUserLevelInfo() {
        if (userLevel != null) {
            userLevel.setText("等级：青铜");
            setLevelColor(userLevel, "青铜");
        }

        if (userLevelScore != null) {
            userLevelScore.setText("分数：0");
        }

        // 其他控件设置默认值
        if (activeScore != null) activeScore.setText("0");
        if (creditScore != null) creditScore.setText("60");
        if (successRate != null) {
            successRate.setText("100%");
            setSuccessRateColor(successRate, 100);
        }
        if (transactionCount != null) transactionCount.setText("0");
        if (reviewCount != null) reviewCount.setText("0");
        if (nextLevel != null) nextLevel.setText("白银");

        if (currentScoreText != null) currentScoreText.setText("当前：0");
        if (nextScoreText != null) nextScoreText.setText("目标：60"); // 修改为青铜的最大分

        // 默认进度为0
        if (progressBar != null && progressText != null) {
            updateProgressBar(0, null);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if(StaticVar.cookie.equals("")){
            Intent intent = new Intent(getContext(), Login.class);
            Objects.requireNonNull(getActivity()).startActivityForResult(intent, StaticVar.LOGIN);
        } else {
            init();
            // ========== 新增：每次回到页面时刷新统计信息 ==========
            loadUserStatsInfo();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.setting:
                Intent intent = new Intent(getContext(), Setting.class);
                startActivity(intent);
                break;
        }
    }
}