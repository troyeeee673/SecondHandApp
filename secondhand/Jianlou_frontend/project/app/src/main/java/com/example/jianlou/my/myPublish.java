package com.example.jianlou.my;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;
import com.example.jianlou.staticVar.Table;

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

public class myPublish extends AppCompatActivity implements View.OnClickListener {
    private ImageView back;
    private RecyclerView recyclerView;
    private final List<Publish> goodList = new ArrayList<>();
    private PublishAdapter goodAdapter;

    private static final String BACKEND_BASE_URL = "http://192.168.34.31:8080";
    private static final String DELETE_URL = BACKEND_BASE_URL + "/delete_goods/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_publish);
        init();
    }

    private void init() {
        back = findViewById(R.id.my_publish_back);
        recyclerView = findViewById(R.id.my_publish_recycle);
        back.setOnClickListener(this);

        // 初始化RecyclerView
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);

        // 初始化Adapter
        goodAdapter = new PublishAdapter(goodList);
        recyclerView.setAdapter(goodAdapter);

        // 设置删除监听器
        goodAdapter.setOnItemDeleteListener(new PublishAdapter.OnItemDeleteListener() {
            @Override
            public void onItemDelete(String goodID, int position) {
                deleteGood(goodID, position);
            }
        });

        initGood();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.my_publish_back) {
            finish();
        }
    }

    private void initGood() {
        goodList.clear();
        RequestBody requestBody = new FormBody.Builder()
                .add(Table.cookie, StaticVar.cookie)
                .build();

        HttpUtil.sendOkHttpRequest(StaticVar.MyUrl, requestBody, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(myPublish.this, "请求失败，请检查网络", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(response);
            }
        });
    }

    private void deleteGood(String goodID, int position) {
        // 先检查商品是否已售出
        if (position >= 0 && position < goodList.size()) {
            Publish publish = goodList.get(position);
            if (publish.isSold()) {
                runOnUiThread(() -> Toast.makeText(myPublish.this, "已售出的商品不能删除", Toast.LENGTH_SHORT).show());
                return;
            }
        }

        // 立即从UI中移除，提供更好的用户体验
        goodAdapter.removeItem(position);

        // 向后端发送删除请求
        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("goodsID", goodID)
                .build();

        HttpUtil.sendOkHttpRequest(DELETE_URL, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(myPublish.this, "删除失败，请检查网络", Toast.LENGTH_SHORT).show();
                    // 如果网络失败，重新加载数据以恢复UI
                    initGood();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if ("success".equals(responseData)) {
                        Toast.makeText(myPublish.this, "删除成功", Toast.LENGTH_SHORT).show();
                        // 从列表中移除已删除的商品
                        for (int i = 0; i < goodList.size(); i++) {
                            if (goodList.get(i).getGoodID().equals(goodID)) {
                                goodList.remove(i);
                                break;
                            }
                        }
                        // 通知Adapter数据已变化
                        goodAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(myPublish.this, "删除失败：" + responseData, Toast.LENGTH_SHORT).show();
                        // 如果后端删除失败，重新加载数据以恢复UI
                        initGood();
                    }
                });
            }
        });
    }

    private void handleResponse(Response response) throws IOException {
        if (response.code() != 200) {
            runOnUiThread(() -> Toast.makeText(myPublish.this, "服务器故障：" + response.code(), Toast.LENGTH_SHORT).show());
            return;
        }
        if (response.body() == null) {
            runOnUiThread(() -> Toast.makeText(myPublish.this, "服务器返回空数据", Toast.LENGTH_SHORT).show());
            return;
        }

        String responseData = response.body().string();
        if ("failed".equals(responseData)) {
            runOnUiThread(() -> Toast.makeText(myPublish.this, "暂无发布的商品", Toast.LENGTH_SHORT).show());
            return;
        }

        try {
            JSONArray jsonArray = new JSONArray(responseData);
            goodList.clear();

            runOnUiThread(() -> {
                if (jsonArray.length() > 0) {
                    Toast.makeText(myPublish.this, "共加载" + jsonArray.length() + "个商品", Toast.LENGTH_SHORT).show();
                }
            });

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String money = jsonObject.optString("money", "0");
                String content = jsonObject.optString("content", "暂无描述");
                String goodID = jsonObject.optString("goodsID", "");
                String imagePath = jsonObject.optString("image", "");
                String status = jsonObject.optString("status", "on_sale"); // 获取商品状态

                String finalImageUrl = getFinalImageUrl(imagePath);
                // 使用新的构造函数，包含status参数
                Publish publish = new Publish(Uri.parse(finalImageUrl), content, money, goodID, status);
                goodList.add(publish);
            }

            runOnUiThread(() -> goodAdapter.notifyDataSetChanged());

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(myPublish.this, "数据解析失败", Toast.LENGTH_SHORT).show());
        }
    }

    private String getFinalImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "android.resource://" + getPackageName() + "/" + R.mipmap.ic_launcher;
        }

        // 如果已经是完整的URL，直接返回
        if (imagePath.startsWith("http")) {
            return imagePath;
        }

        // 如果是相对路径（以/upload/开头），拼接完整URL
        if (imagePath.startsWith("/upload/")) {
            return BACKEND_BASE_URL + imagePath;
        }

        // 其他情况，尝试拼接
        return BACKEND_BASE_URL + "/upload/" + imagePath;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 每次回到页面时刷新数据
        initGood();
    }
}