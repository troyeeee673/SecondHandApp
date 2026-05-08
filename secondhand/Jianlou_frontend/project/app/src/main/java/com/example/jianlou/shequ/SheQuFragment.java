package com.example.jianlou.shequ;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Response;

/**
 * 社区界面
 */
public class SheQuFragment extends Fragment {
    // 缓存Fragment view
    private View rootView;
    private List<Post> postList = new ArrayList<>();
    private TextView tvPost; // 发布按钮
    private PostAdapter postAdapter; // 适配器（全局引用，便于刷新）
    private RecyclerView recyclerView; // RecyclerView（全局引用）

    public SheQuFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_shequ, container, false);
            // 绑定发布按钮（仅在View创建时绑定一次）
            tvPost = rootView.findViewById(R.id.post);
            // 绑定RecyclerView（全局引用）
            recyclerView = rootView.findViewById(R.id.shequ_recycler_view);
            // 初始化RecyclerView布局
            initRecyclerViewLayout();
            // 添加发布按钮点击事件
            setPostButtonClickListener();
        }

        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null) {
            parent.removeView(rootView);
        }
        return rootView;
    }

    /**
     * 初始化RecyclerView布局（仅执行一次）
     */
    private void initRecyclerViewLayout() {
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        // 1.解决瀑布流错位问题（可选，优化体验）
        layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_NONE);
        // 2. 自动测量，适配wrap_content高度
        layoutManager.setAutoMeasureEnabled(true);
        recyclerView.setLayoutManager(layoutManager);
        // 3. 优化性能：固定RecyclerView大小
        recyclerView.setHasFixedSize(true);
        // 初始化适配器
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);
    }

    /**
     * 发布按钮点击事件：跳转至帖子发布页面
     */
    private void setPostButtonClickListener() {
        tvPost.setOnClickListener(v -> {
            // 非空判断：避免getActivity()为null
            if (getActivity() == null) {
                return;
            }
            // 跳转到PublishPostActivity
            Intent intent = new Intent(getActivity(), PublishPostActivity.class);
            // 可选：设置请求码，用于返回后处理
            startActivityForResult(intent, StaticVar.POST_PUBLISH_REQUEST);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // 页面显示时，请求最新帖子列表
        loadRealPostList();
    }

    /**
     * 接收帖子发布成功后的返回结果（优化：直接添加新帖子，无需重新请求后端）
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 非空判断：避免getActivity()为null
        if (getActivity() == null) {
            return;
        }

        if (requestCode == StaticVar.POST_PUBLISH_REQUEST && resultCode == getActivity().RESULT_OK) {
            try {
                // 1. 接收PublishPostActivity返回的带后端postId的Post对象
                Post newPost = (Post) data.getSerializableExtra("newPost");
                if (newPost != null && !TextUtils.isEmpty(newPost.getPostId())) {
                    // 2. 添加到列表顶部（无需重新请求后端，提升体验）
                    postList.add(0, newPost);
                    // 3. 通知适配器局部刷新（比notifyDataSetChanged更高效）
                    postAdapter.notifyItemInserted(0);
                    // 4. 滚动到列表顶部，展示新发布的帖子
                    recyclerView.scrollToPosition(0);
                    // 5. 提示用户
                   // Toast.makeText(getActivity(), "帖子发布成功", Toast.LENGTH_SHORT).show();
                } else {
                    // 若接收失败，重新请求后端列表（兜底方案）
                   // Toast.makeText(getActivity(), "帖子发布成功，正在刷新列表...", Toast.LENGTH_SHORT).show();
                    loadRealPostList();
                }
            } catch (Exception e) {
                e.printStackTrace();
                // 异常时兜底刷新
                //Toast.makeText(getActivity(), "帖子发布成功，正在刷新列表...", Toast.LENGTH_SHORT).show();
                loadRealPostList();
            }
        }
    }

    /**
     * 核心：加载后端真实帖子列表（补充postId解析，修复字段映射）
     */
    private void loadRealPostList() {
        // 1. 获取登录后的Cookie（从SharedPreferences中读取）
        String cookie = getCookieFromSP();
        // 非空判断：避免getActivity()为null
        if (getActivity() == null) {
            return;
        }
//        if (cookie == null || cookie.isEmpty()) {
//            Toast.makeText(getActivity(), "请先登录", Toast.LENGTH_SHORT).show();
//            return;
//        }

        // 2. 构建请求参数（Cookie可选传递，有则添加，无则不添加）
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        if (cookie != null && !cookie.isEmpty()) {
            formBodyBuilder.add("cookie", cookie); // 有Cookie才传递，无则不传
        }
        FormBody formBody = formBodyBuilder.build();

        // 3. 后端帖子列表接口地址（与PostController一致，需配置正确的BASE_URL）
        String postListUrl = StaticVar.BaseUrl + "/post/list/"; // 确保BASE_URL是后端真实IP+端口

        // 4. 发起POST网络请求（按HttpUtil规范调用，无需手动构建Request）
        HttpUtil.sendOkHttpRequest(postListUrl, formBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 网络请求失败，切换到主线程提示
                if (getActivity() == null) {
                    return;
                }
                getActivity().runOnUiThread(() -> {
                   // Toast.makeText(getActivity(), "获取帖子列表失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 非空判断：避免getActivity()为null
                if (getActivity() == null) {
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    // 5. 解析后端返回的JSON数据
                    String responseData = response.body().string();
                    // 新增日志：打印后端原始数据，确认是否拿到数据
                    System.out.println("【前端日志】后端返回的帖子原始数据：" + responseData);
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                    List<Map<String, Object>> backendPostList = gson.fromJson(responseData, listType);
                    // 新增日志：打印解析后的帖子数量
                    System.out.println("【前端日志】解析后帖子数量：" + (backendPostList == null ? 0 : backendPostList.size()));

                    // 6. 转换为前端Post列表（补充postId映射，修正字段映射）
                    List<Post> realPostList = new ArrayList<>();
                    // 避免backendPostList为null导致空指针
                    if (backendPostList != null && !backendPostList.isEmpty()) {
                        for (Map<String, Object> backendPost : backendPostList) {
                            // 正确映射后端字段（核心：添加postId解析）
                            String postId = (String) backendPost.get("postId"); // 解析后端返回的postId
                            String postTitle = (String) backendPost.get("postTitle");
                            String postContent = (String) backendPost.get("postContent");
                            String postType = (String) backendPost.get("postType");
                            // 后端postCommunity → 前端postOrigin
                            String postCommunity = (String) backendPost.get("postCommunity");
                            // 后端userName → 前端postUser_name
                            String postUser_name = (String) backendPost.get("userName");
                            // 后端userHead → 前端postHeadUrl
                            String postHeadUrl = (String) backendPost.get("userHead");

                            // 后端返回图片列表，取第一张图片的URL（前端是单图展示）
                            String postPhotoUrl = "";
                            List<Map<String, Object>> imageList = (List<Map<String, Object>>) backendPost.get("imageList");
                            if (imageList != null && !imageList.isEmpty()) {
                                postPhotoUrl = (String) imageList.get(0).get("imageUrl");
                            }

                            String postOrigin = "来自" + (postCommunity != null ? postCommunity : "未知") + "社区";

                            // 后端未返回点赞数和话题，给默认值
                            String postLove = backendPost.get("postLove") != null ? backendPost.get("postLove").toString() : "0";
                            String postTalk = (postType != null) ? "#" + postType + "#" : "#默认话题#";

                            // 创建前端Post对象
                            Post post = new Post(postPhotoUrl, postHeadUrl, postUser_name, postLove, postTalk, postContent, postOrigin);
                            // 核心：给Post对象设置后端返回的postId（确保跳转时能获取到有效id）
                            if (postId != null) {
                                post.setPostId(postId);
                                System.out.println("【前端日志】解析到帖子ID：" + postId);
                            }
                            // 设置帖子标题
                            if (postTitle != null) {
                                post.setPostTitle(postTitle);
                            }
                            realPostList.add(post);
                        }
                    }

                    // 7. 切换到主线程，更新RecyclerView
                    getActivity().runOnUiThread(() -> {
                        System.out.println("【前端日志】准备更新的帖子数量：" + realPostList.size());
                        postList.clear(); // 清空旧数据
                        postList.addAll(realPostList); // 添加新数据
                        postAdapter.notifyDataSetChanged(); // 刷新适配器
                        recyclerView.scrollToPosition(0); // 回到顶部，解决错位
                        // 移除不必要的requestLayout，避免布局混乱
                        // recyclerView.requestLayout();
                        // 提示用户获取成功
                       // Toast.makeText(getActivity(), "共获取到 " + realPostList.size() + " 条帖子", Toast.LENGTH_SHORT).show();
                    });

                } else {
                    // 接口返回失败（打印响应码，便于排查）
                    System.out.println("【前端日志】接口返回异常，响应码：" + response.code());
                    getActivity().runOnUiThread(() -> {
                       // Toast.makeText(getActivity(), "获取帖子列表失败：接口返回异常（响应码：" + response.code() + "）", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    /**
     * 从SharedPreferences中获取登录后的Cookie
     */
    private String getCookieFromSP() {
        // 非空判断：避免getActivity()为null
        if (getActivity() == null) {
            return "";
        }
        return getActivity().getSharedPreferences(StaticVar.fileName, getActivity().MODE_PRIVATE)
                .getString(StaticVar.fileCookiename, "");
    }

//    // 补充：导入TextUtils（避免忘记导入）
//    private static class TextUtils {
//        public static boolean isEmpty(CharSequence str) {
//            return str == null || str.length() == 0;
//        }
//    }
}