package com.example.jianlou.my;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.jianlou.Internet.HttpUtil;
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

public class mySold extends AppCompatActivity implements View.OnClickListener {
    private ImageView back;
    private RecyclerView recyclerView;
    private List<Sold> soldList;
    private SoldAdapter soldAdapter;
    private boolean isLoading = false;

    private static final String BACKEND_BASE_URL = "http://192.168.93.1:8080";
    private static final String MY_SOLD_URL = BACKEND_BASE_URL + "/orders/my_sold/";
    private static final String TAG = "MySoldActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_sold);
        init();
    }

    private void init() {
        Log.d(TAG, "初始化Activity");

        back = findViewById(R.id.my_sold_back);
        recyclerView = findViewById(R.id.my_sold_recycle);
        back.setOnClickListener(this);

        soldList = new ArrayList<>();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        soldAdapter = new SoldAdapter(soldList);
        recyclerView.setAdapter(soldAdapter);

        Log.d(TAG, "RecyclerView 和 Adapter 初始化完成");

        if (!isLoading) {
            loadSoldOrders();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.my_sold_back) {
            finish();
        }
    }

    private void loadSoldOrders() {
        if (isLoading) {
            Log.d(TAG, "已经在加载中，跳过");
            return;
        }

        isLoading = true;
        Log.d(TAG, "开始加载已售出订单数据");

        if (StaticVar.cookie == null || StaticVar.cookie.isEmpty()) {
            Log.e(TAG, "Cookie为空，用户未登录");
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            isLoading = false;
            return;
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .build();

        Log.d(TAG, "发送请求到: " + MY_SOLD_URL);

        HttpUtil.sendOkHttpRequest(MY_SOLD_URL, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isLoading = false;
                Log.e(TAG, "请求失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(mySold.this, "请求失败，请检查网络: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isLoading = false;
                Log.d(TAG, "收到响应，状态码: " + response.code());
                handleResponse(response);
            }
        });
    }

    private void handleResponse(Response response) throws IOException {
        if (response.code() != 200) {
            Log.e(TAG, "服务器故障，状态码: " + response.code());
            runOnUiThread(() -> Toast.makeText(mySold.this, "服务器故障：" + response.code(), Toast.LENGTH_SHORT).show());
            return;
        }

        if (response.body() == null) {
            Log.e(TAG, "响应体为空");
            runOnUiThread(() -> Toast.makeText(mySold.this, "服务器返回空数据", Toast.LENGTH_SHORT).show());
            return;
        }

        String responseData = response.body().string();
        Log.d(TAG, "响应数据: " + responseData);

        try {
            JSONObject jsonObject = new JSONObject(responseData);
            String status = jsonObject.optString("status", "error");

            if ("error".equals(status)) {
                String message = jsonObject.optString("message", "加载失败");
                Log.e(TAG, "服务器返回错误: " + message);
                runOnUiThread(() -> Toast.makeText(mySold.this, message, Toast.LENGTH_SHORT).show());
                return;
            }

            JSONArray jsonArray = jsonObject.optJSONArray("orders");
            if (jsonArray == null || jsonArray.length() == 0) {
                Log.d(TAG, "没有已售出订单数据");
                runOnUiThread(() -> {
                    Toast.makeText(mySold.this, "暂无售出记录", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            Log.d(TAG, "解析到 " + jsonArray.length() + " 个已售出订单");

            List<Sold> newSoldList = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject orderObj = jsonArray.getJSONObject(i);

                String price = orderObj.optString("price", "0");
                String goodsName = orderObj.optString("goods_name", "商品");
                String orderId = orderObj.optString("order_id", "");
                String imagePath = orderObj.optString("goods_image", "");
                String buyerName = orderObj.optString("buyer_name", "买家");
                String buyerAccount = orderObj.optString("buyer_account", "");
                String createTime = orderObj.optString("create_time", "");
                String goodsId = orderObj.optString("goods_id", "");

                // 关键：获取订单状态
                String orderStatus = orderObj.optString("status", "pending");
                Log.d(TAG, "已售出订单 " + i + " 状态: " + orderStatus);

                String finalImageUrl = getFinalImageUrl(imagePath);
                Uri imageUri = Uri.parse(finalImageUrl);

                Sold sold = new Sold(imageUri, goodsName, "¥" + price, orderId,
                        buyerName, buyerAccount, createTime, goodsId, orderStatus);
                newSoldList.add(sold);
            }

            Log.d(TAG, "解析完成，共 " + newSoldList.size() + " 个已售出订单");

            runOnUiThread(() -> {
                if (soldList != null) {
                    soldList.clear();
                    soldList.addAll(newSoldList);
                }

                if (soldAdapter != null) {
                    soldAdapter.updateData(newSoldList);
                    Log.d(TAG, "适配器已更新");

                    if (newSoldList.isEmpty()) {
                        Toast.makeText(mySold.this, "暂无售出记录", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mySold.this, "共加载" + newSoldList.size() + "条售出记录", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "适配器为null");
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON解析失败: " + e.getMessage());
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(mySold.this, "数据解析失败", Toast.LENGTH_SHORT).show());
        }
    }

    private String getFinalImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.d(TAG, "图片路径为空，使用默认图片");
            return "android.resource://" + getPackageName() + "/" + R.mipmap.ic_launcher;
        }

        if (imagePath.startsWith("http")) {
            return imagePath;
        }

        if (imagePath.startsWith("/upload/")) {
            return BACKEND_BASE_URL + imagePath;
        }

        return BACKEND_BASE_URL + "/upload/" + imagePath;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }
}