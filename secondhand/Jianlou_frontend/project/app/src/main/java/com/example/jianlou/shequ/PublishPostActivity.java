package com.example.jianlou.shequ;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.Login.Login;
import com.example.jianlou.R;
import com.example.jianlou.publish.publishGood.FullyGridLayoutManager;
import com.example.jianlou.publish.publishGood.GridImageAdapter;
import com.example.jianlou.staticVar.StaticVar;
import com.example.jianlou.staticVar.Table;
import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureConfig;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.permissions.RxPermissions;
import com.luck.picture.lib.tools.PictureFileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;

// 参照PublishGoodActivity实现，移除商品特有字段（价格、运费、分类），保留帖子核心字段
public class PublishPostActivity extends AppCompatActivity implements View.OnClickListener {
    // 相册选择图片列表
    private List<LocalMedia> selectList = new ArrayList<>();
    private GridImageAdapter adapter;
    private int maxSelectNum = 9; // 最大图片选择数，与商品发布一致

    // 控件声明
    private EditText etPostTitle; // 帖子标题
    private EditText etPostContent; // 帖子内容
    private ProgressBar progressBar;
    private TextView tvPostType; // 帖子类型（可选，如动态、求助、分享）
    private String[] postTypeResult; // 帖子类型返回值

    private RelativeLayout rlPostCommunity; // 社区选择布局
    private TextView tvPostCommunity; // 显示选中的社区
    private String[] postCommunityResult; // 社区返回值

    // 相册配置参数
    private int themeId;
    private int chooseMode = PictureMimeType.ofAll();
    private int statusBarColorPrimaryDark;
    private int upResId, downResId;

    // 新增：本地Post对象引用，用于存储带后端postId的帖子信息
    private Post newLocalPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 先验证登录状态，与商品发布逻辑一致
        if (StaticVar.cookie.equals("")) {
            Intent intent = new Intent(PublishPostActivity.this, Login.class);
            startActivityForResult(intent, StaticVar.LOGIN);
        } else {
            setContentView(R.layout.activity_publish_post);
            initView(); // 初始化控件
        }
        SharedPreferences sp = getSharedPreferences(StaticVar.fileName, MODE_PRIVATE);
        String savedCookie = sp.getString(StaticVar.fileCookiename, ""); // 确保StaticVar中有fileCookie常量
        if (!savedCookie.isEmpty()) {
            StaticVar.cookie = savedCookie;
            Log.d("PostPublish_Init", "从SharedPreferences恢复的Cookie：" + StaticVar.cookie);
        }

    }

    /**
     * 初始化控件和事件绑定（参照商品发布的init方法）
     */
    private void initView() {
        // 绑定控件
        RecyclerView rvAddPhoto = findViewById(R.id.PublishPost_addPhoto);
        ImageView ivBack = findViewById(R.id.PublishPost_back);
        RelativeLayout rlPostType = findViewById(R.id.PublishPost_type);
        Button btnPublish = findViewById(R.id.PublishPost_publish);
        progressBar = findViewById(R.id.publish_post_bar);
        etPostTitle = findViewById(R.id.PublishPost_title);
        etPostContent = findViewById(R.id.PublishPost_content);
        tvPostType = findViewById(R.id.PublishPost_type_1);
        rlPostCommunity = findViewById(R.id.PublishPost_community);
        tvPostCommunity = findViewById(R.id.PublishPost_community_1);

        // 设置点击监听
        ivBack.setOnClickListener(this);
        btnPublish.setOnClickListener(this);
        rlPostType.setOnClickListener(this);
        rlPostCommunity.setOnClickListener(this);

        // 初始化相册RecyclerView（与商品发布完全一致）
        FullyGridLayoutManager manager = new FullyGridLayoutManager(PublishPostActivity.this, 3, GridLayoutManager.VERTICAL, false);
        rvAddPhoto.setLayoutManager(manager);
        adapter = new GridImageAdapter(PublishPostActivity.this, onAddPicClickListener);
        adapter.setList(selectList);
        adapter.setSelectMax(maxSelectNum);
        rvAddPhoto.setAdapter(adapter);

        // 初始化相册配置
        initPictureConfig();
    }

    /**
     * 相册配置（完全复用商品发布的相册逻辑）
     */
    private void initPictureConfig() {
        themeId = R.style.picture_QQ_style;
        statusBarColorPrimaryDark = R.color.blue;
        upResId = R.drawable.arrow_up;
        downResId = R.drawable.arrow_down;

        // 图片预览点击事件
        adapter.setOnItemClickListener((position, v) -> {
            if (selectList.size() > 0) {
                LocalMedia media = selectList.get(position);
                String mimeType = media.getMimeType();
                int mediaType = PictureMimeType.getMimeType(mimeType);
                switch (mediaType) {
                    case PictureConfig.TYPE_VIDEO:
                        PictureSelector.create(PublishPostActivity.this).externalPictureVideo(media.getPath());
                        break;
                    case PictureConfig.TYPE_AUDIO:
                        PictureSelector.create(PublishPostActivity.this).externalPictureAudio(media.getPath());
                        break;
                    default:
                        PictureSelector.create(PublishPostActivity.this)
                                .themeStyle(themeId)
                                .openExternalPreview(position, selectList);
                        break;
                }
            }
        });

        // 清空图片缓存（需权限）
        RxPermissions permissions = new RxPermissions(this);
        permissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(new Observer<Boolean>() {
            @Override
            public void onSubscribe(Disposable d) {}

            @Override
            public void onNext(Boolean aBoolean) {
                if (aBoolean) {
                    PictureFileUtils.deleteCacheDirFile(PublishPostActivity.this, PictureMimeType.ofImage());
                } else {
                    Toast.makeText(PublishPostActivity.this,
                            getString(R.string.picture_jurisdiction), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onComplete() {}
        });
    }

    /**
     * 相册添加图片点击事件（与商品发布一致）
     */
    private GridImageAdapter.onAddPicClickListener onAddPicClickListener = new GridImageAdapter.onAddPicClickListener() {
        @Override
        public void onAddPicClick() {
            PictureSelector.create(PublishPostActivity.this)
                    .openGallery(chooseMode)
                    .theme(themeId)
                    .maxSelectNum(maxSelectNum)
                    .minSelectNum(0) // 帖子允许无图片
                    .imageSpanCount(4)
                    .cameraFileName("")
                    .selectionMode(PictureConfig.MULTIPLE)
                    .isSingleDirectReturn(false)
                    .previewImage(true)
                    .previewVideo(true)
                    .enablePreviewAudio(false)
                    .isCamera(true)
                    .isChangeStatusBarFontColor(false)
                    .setStatusBarColorPrimaryDark(statusBarColorPrimaryDark)
                    .setUpArrowDrawable(upResId)
                    .setDownArrowDrawable(downResId)
                    .isOpenStyleCheckNumMode(true)
                    .isZoomAnim(true)
                    .enableCrop(false)
                    .compress(true)
                    .synOrAsy(false)
                    .glideOverride(160, 160)
                    .isGif(true)
                    .openClickSound(false)
                    .selectionMedia(selectList)
                    .cutOutQuality(90)
                    .minimumCompressSize(100)
                    .forResult(PictureConfig.CHOOSE_REQUEST);
        }
    };

    /**
     * 点击事件处理（参照商品发布的onClick方法）
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.PublishPost_publish:
                validateAndPublish(); // 验证并发布帖子
                break;
            case R.id.PublishPost_back:
                // 返回上一页，取消发布
                Intent intent = new Intent();
                setResult(RESULT_CANCELED, intent);
                finish();
                break;
            case R.id.PublishPost_type:
                // 跳转帖子类型选择页面（参照商品分类逻辑）
                Intent typeIntent = new Intent(PublishPostActivity.this, PublishPostType.class);
                if (postTypeResult != null) {
                    typeIntent.putExtra("postType", postTypeResult);
                }
                startActivityForResult(typeIntent, StaticVar.PublishPostTypeNUM);
                break;
            case R.id.PublishPost_community:
                Intent communityIntent = new Intent(PublishPostActivity.this, PublishPostCommunity.class);
                if (postCommunityResult != null) {
                    communityIntent.putExtra("postCommunity", postCommunityResult);
                }
                startActivityForResult(communityIntent, StaticVar.PublishPostCommunityNUM); // 新增请求码
                break;
        }
    }

    /**
     * 处理页面返回结果（参照商品发布的onActivityResult方法）
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PictureConfig.CHOOSE_REQUEST:
                    // 相册选择返回
                    selectList = PictureSelector.obtainMultipleResult(data);
                    adapter.setList(selectList);
                    adapter.notifyDataSetChanged();
                    break;
                case StaticVar.PublishPostTypeNUM:
                    // 帖子类型选择返回
                    postTypeResult = data.getStringArrayExtra("postType");
                    if (postTypeResult != null) {
                        StringBuilder typeStr = new StringBuilder();
                        for (int i = 0; i < postTypeResult.length; i++) {
                            typeStr.append(postTypeResult[i]);
                            if (i != postTypeResult.length - 1) {
                                typeStr.append(",");
                            }
                        }
                        tvPostType.setTextColor(Color.rgb(0, 0, 0));
                        tvPostType.setText(typeStr.toString());
                    }
                    break;
                case StaticVar.PublishPostCommunityNUM:
                    postCommunityResult = data.getStringArrayExtra("postCommunity");
                    if (postCommunityResult != null) {
                        StringBuilder communityStr = new StringBuilder();
                        for (int i = 0; i < postCommunityResult.length; i++) {
                            communityStr.append(postCommunityResult[i]);
                            if (i != postCommunityResult.length - 1) {
                                communityStr.append(",");
                            }
                        }
                        tvPostCommunity.setTextColor(Color.rgb(0, 0, 0));
                        tvPostCommunity.setText(communityStr.toString());
                    }
                    break;
                case StaticVar.LOGIN:
                    // 登录成功后重新初始化
                    setContentView(R.layout.activity_publish_post);
                    initView();
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            if (requestCode == StaticVar.LOGIN) {
                finish(); // 登录取消，关闭发布页面
            }
        }
    }

    /**
     * 发布前校验（参照商品发布的PublishGood_publish方法）
     */
    private void validateAndPublish() {
        String title = etPostTitle.getText().toString().trim();
        String content = etPostContent.getText().toString().trim();
        String postType = tvPostType.getText().toString().trim();
        String postCommunity = tvPostCommunity.getText().toString().trim();

        // 校验逻辑（按需调整，帖子允许无图片）
        if (title.length() == 0) {
            Toast.makeText(this, "请填写帖子标题", Toast.LENGTH_SHORT).show();
        } else if (content.length() == 0) {
            Toast.makeText(this, "请填写帖子内容", Toast.LENGTH_SHORT).show();
        } else if (postType.length() == 0) {
            Toast.makeText(this, "请选择帖子类型", Toast.LENGTH_SHORT).show();
        } else if (postCommunity.length() == 0) { // 新增社区校验
            Toast.makeText(this, "请选择所属社区", Toast.LENGTH_SHORT).show();
        } else {
            // 校验通过，提交数据
            postPostData();
        }
    }

    /**
     * 提交帖子数据到服务器（后端生成postId，前端接收并赋值）
     */
    private void postPostData() {
        progressBar.setVisibility(View.VISIBLE);
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        Map<String, String> params = new HashMap<>();

        // ========== 关键修改：移除前端UUID生成postId，不传递postId参数 ==========
        // 帖子核心参数（仅传递业务数据，不含postId）
        params.put(Table.cookie, StaticVar.cookie);
        params.put(Table.postTitle, etPostTitle.getText().toString().trim());
        params.put(Table.postContent, etPostContent.getText().toString().trim());
        params.put(Table.postType, tvPostType.getText().toString().trim());
        params.put(Table.postCommunity, tvPostCommunity.getText().toString().trim());
        params.put(Table.postTime, String.valueOf(new Date()));

        Log.d("PostPublish_Params", "前端准备传递的参数（无本地postId）：");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            Log.d("PostPublish_Params", entry.getKey() + " = " + entry.getValue());
        }

        // 构建图片文件列表（兼容Android Q）
        List<File> fileList = new ArrayList<>();
        for (LocalMedia media : selectList) {
            String path = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                path = media.getAndroidQToPath();
            } else {
                path = media.getPath();
            }
            if (path == null || path.isEmpty()) {
                continue;
            }
            File file = new File(path);
            if (file.exists()) {
                fileList.add(file);
            }
        }

        Log.d("PostPublish_File", "要上传的图片数量：" + fileList.size());
        for (int i = 0; i < fileList.size(); i++) {
            Log.d("PostPublish_File", "第" + (i+1) + "张图片路径：" + fileList.get(i).getAbsolutePath());
        }

        // 添加文本参数到请求体
        try {
            if (params != null && !params.isEmpty()) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    bodyBuilder.addFormDataPart(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "参数构建失败", Toast.LENGTH_SHORT).show();
            return;
        }

        // 添加图片文件到请求体
        if (fileList != null && !fileList.isEmpty()) {
            for (File f : fileList) {
                String mimeType = getMimeType(f.getName());
                bodyBuilder.addFormDataPart("postImage", f.getName(),
                        RequestBody.create(MediaType.parse(mimeType), f));
            }
        }

        // 发送网络请求（使用与商品发布相同的HttpUtil）
        HttpUtil.sendOkHttpRequest(StaticVar.postPublishUrl, bodyBuilder.build(), new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateProgressBar();
                showToast("发布失败，请检查网络");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                updateProgressBar();
                if (response.code() == 200 && response.body() != null) {
                    String responseData = response.body().string();
                    Log.d("PostPublish_Response", "后端返回数据：" + responseData);

                    // ========== 关键：解析后端返回的postId ==========
                    try {
                        // 后端返回格式：{"code":"success","postId":"xxx","msg":"发布成功"}
                        JSONObject jsonObject = new JSONObject(responseData);
                        String serverPostId = jsonObject.optString("postId");
                        String code = jsonObject.optString("code");

                        if ("success".equals(code) && !TextUtils.isEmpty(serverPostId)) {
                            runOnUiThread(() -> {
                                // 1. 创建本地Post对象，赋值后端返回的postId
                                newLocalPost = createLocalPostObject(serverPostId);
                                // 2. 将带后端postId的Post对象回传给列表页
                                Intent intent = new Intent();
                                intent.putExtra("newPost", newLocalPost); // 传递序列化对象
                                setResult(RESULT_OK, intent);

                                Toast.makeText(PublishPostActivity.this, "帖子发布成功", Toast.LENGTH_SHORT).show();
                                finish(); // 发布成功，关闭页面
                            });
                        } else {
                            String errorMsg = jsonObject.optString("msg", "未知错误");
                            showToast("发布失败：" + errorMsg);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        showToast("解析后端返回数据失败");
                    }
                } else {
                    showToast("服务器故障：" + response.code());
                }
            }
        });
    }

    /**
     * 创建本地Post对象（使用后端返回的postId）
     */
    private Post createLocalPostObject(String serverPostId) {
        Post post = new Post();
        // 赋值后端返回的postId（核心）
        post.setPostId(serverPostId);
        // 设置当前登录用户信息
        post.setPostUser_name(StaticVar.fileUserName != null ? StaticVar.fileUserName : "未知用户");
        post.setPostHeadUrl(StaticVar.fileHeadUrl);
        post.setPostHeadID(-1); // 标记为网络头像
        // 设置帖子业务信息
        post.setPostTitle(etPostTitle.getText().toString().trim());
        post.setPostContent(etPostContent.getText().toString().trim());
        post.setPostTalk(tvPostType.getText().toString().trim());
        post.setPostOrigin(tvPostCommunity.getText().toString().trim());
        post.setPostLove("0"); // 初始点赞数为0
        // 设置帖子图片
        if (!selectList.isEmpty()) {
            LocalMedia firstMedia = selectList.get(0);
            String imgPath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? firstMedia.getAndroidQToPath() : firstMedia.getPath();
            post.setPostPhotoUrl(imgPath);
            post.setPostPhotoID(-1); // 标记为网络图片
        } else {
            post.setPostPhotoUrl("");
            post.setPostPhotoID(R.mipmap.shequ0); // 无图片时使用默认图
        }
        return post;
    }

    /**
     * 获取文件MIME类型（复用商品发布的方法）
     */
    private String getMimeType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else {
            return "image/*";
        }
    }

    /**
     * 更新进度条（切换到主线程）
     */
    private void updateProgressBar() {
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
    }

    /**
     * 显示Toast（切换到主线程）
     */
    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(PublishPostActivity.this, message, Toast.LENGTH_SHORT).show());
    }
}