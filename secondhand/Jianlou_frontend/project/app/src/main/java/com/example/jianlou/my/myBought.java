package com.example.jianlou.my;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog; // 导入原生AlertDialog
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;
// 移除MaterialDialog导入：import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

public class myBought extends AppCompatActivity implements View.OnClickListener {
    private ImageView back;
    private RecyclerView recyclerView;
    private List<Bought> boughtList;
    private BoughtAdapter boughtAdapter;
    private boolean isLoading = false;

    private static final String BACKEND_BASE_URL = "http://192.168.34.31:8080";
    private static final String MY_BOUGHT_URL = BACKEND_BASE_URL + "/orders/my_bought/";
    private static final String COMPLETE_ORDER_URL = BACKEND_BASE_URL + "/orders/complete/";
    private static final String RETURN_ORDER_URL = BACKEND_BASE_URL + "/orders/return/";
    private static final String TAG = "MyBoughtActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bought);
        init();
    }

    private void init() {
        Log.d(TAG, "初始化Activity");

        back = findViewById(R.id.my_bought_back);
        recyclerView = findViewById(R.id.my_bought_recycle);
        back.setOnClickListener(this);

        // 初始化列表
        boughtList = new ArrayList<>();

        // 设置RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        // 初始化Adapter，传入Activity引用以便处理按钮点击
        boughtAdapter = new BoughtAdapter(boughtList, this);
        recyclerView.setAdapter(boughtAdapter);

        Log.d(TAG, "activity引用: " + this);
        Log.d(TAG, "适配器activity字段: " + boughtAdapter.getActivityReference()); // 需要添加这个方法

        // 加载数据
        if (!isLoading) {
            loadOrders();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.my_bought_back) {
            finish();
        }
    }

    private void loadOrders() {
        if (isLoading) {
            Log.d(TAG, "已经在加载中，跳过");
            return;
        }

        isLoading = true;
        Log.d(TAG, "开始加载订单数据");

        if (StaticVar.cookie == null || StaticVar.cookie.isEmpty()) {
            Log.e(TAG, "Cookie为空，用户未登录");
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            isLoading = false;
            return;
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .build();

        Log.d(TAG, "发送请求到: " + MY_BOUGHT_URL);

        HttpUtil.sendOkHttpRequest(MY_BOUGHT_URL, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isLoading = false;
                Log.e(TAG, "请求失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(myBought.this, "请求失败，请检查网络: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
            runOnUiThread(() -> Toast.makeText(myBought.this, "服务器故障：" + response.code(), Toast.LENGTH_SHORT).show());
            return;
        }

        if (response.body() == null) {
            Log.e(TAG, "响应体为空");
            runOnUiThread(() -> Toast.makeText(myBought.this, "服务器返回空数据", Toast.LENGTH_SHORT).show());
            return;
        }

        String responseData = response.body().string();
        Log.d(TAG, "响应数据: " + responseData); // 打印完整响应以便调试

        try {
            JSONObject jsonObject = new JSONObject(responseData);
            String status = jsonObject.optString("status", "error");

            if ("error".equals(status)) {
                String message = jsonObject.optString("message", "加载失败");
                Log.e(TAG, "服务器返回错误: " + message);
                runOnUiThread(() -> Toast.makeText(myBought.this, message, Toast.LENGTH_SHORT).show());
                return;
            }

            JSONArray jsonArray = jsonObject.optJSONArray("orders");
            if (jsonArray == null || jsonArray.length() == 0) {
                Log.d(TAG, "没有订单数据");
                runOnUiThread(() -> {
                    Toast.makeText(myBought.this, "暂无购买记录", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            Log.d(TAG, "解析到 " + jsonArray.length() + " 个订单");

            // 创建新的列表来存储解析的数据
            List<Bought> newBoughtList = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject orderObj = jsonArray.getJSONObject(i);

                String price = orderObj.optString("price", "0");
                String goodsName = orderObj.optString("goods_name", "商品");
                String orderId = orderObj.optString("order_id", "");
                String imagePath = orderObj.optString("goods_image", "");
                String sellerName = orderObj.optString("seller_name", "卖家");
                String sellerAccount = orderObj.optString("seller_account", "");
                String createTime = orderObj.optString("create_time", "");
                String goodsId = orderObj.optString("goods_id", "");

                // 从后端获取订单状态 - 这是关键修改！
                String orderStatus = orderObj.optString("status", "pending"); // 读取后端返回的状态
                Log.d(TAG, "订单 " + orderId + " 状态: " + orderStatus);

                String finalImageUrl = getFinalImageUrl(imagePath);
                Uri imageUri = Uri.parse(finalImageUrl);

                Bought bought = new Bought(
                        imageUri,
                        goodsName,
                        "¥" + price,
                        orderId,
                        sellerName,
                        sellerAccount,
                        createTime,
                        goodsId,
                        orderStatus // 传递从后端获取的状态
                );
                newBoughtList.add(bought);
            }

            Log.d(TAG, "解析完成，共 " + newBoughtList.size() + " 个订单");

            runOnUiThread(() -> {
                // 更新boughtList
                if (boughtList != null) {
                    boughtList.clear();
                    boughtList.addAll(newBoughtList);
                }

                // 更新适配器
                if (boughtAdapter != null) {
                    boughtAdapter.updateData(newBoughtList);
                    Log.d(TAG, "适配器已更新，通知数据变化");

                    if (newBoughtList.isEmpty()) {
                        Toast.makeText(myBought.this, "暂无购买记录", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(myBought.this, "共加载" + newBoughtList.size() + "条购买记录", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.e(TAG, "适配器为null");
                }
            });

        } catch (JSONException e) {
            Log.e(TAG, "JSON解析失败: " + e.getMessage());
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(myBought.this, "数据解析失败", Toast.LENGTH_SHORT).show());
        }
    }

    // 处理确认收货按钮点击
// 处理确认收货按钮点击
    public void onConfirmReceiptClick(String orderId, int position) {
        // 先检查订单状态
        if (position >= 0 && position < boughtList.size()) {
            Bought bought = boughtList.get(position);
            String status = bought.getOrderStatus();

            Log.d(TAG, "确认收货点击，订单状态: " + status);

            if ("completed".equals(status)) {
                Toast.makeText(this, "订单已完成，无需重复操作", Toast.LENGTH_SHORT).show();
                return;
            } else if ("returned".equals(status)) {
                Toast.makeText(this, "订单已退货，无法确认收货", Toast.LENGTH_SHORT).show();
                return;
            } else if (!"pending".equals(status)) {
                Toast.makeText(this, "订单状态异常，无法操作", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("确认收货")
                .setMessage("请确认您已收到商品，确认后将完成交易并更新您的活跃度")
                .setPositiveButton("确认收货", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        completeOrder(orderId, position);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }


    // 处理退货按钮点击

    // 处理退货按钮点击
// 处理退货按钮点击
    public void onReturnClick(String orderId, int position) {
        Log.d(TAG, "onReturnClick 被调用，订单ID: " + orderId + ", 位置: " + position);

        // 先检查订单状态
        if (position >= 0 && position < boughtList.size()) {
            Bought bought = boughtList.get(position);
            String status = bought.getOrderStatus();

            Log.d(TAG, "退货点击，订单状态: " + status);

            if ("completed".equals(status)) {
                Toast.makeText(this, "订单已完成，无法退货", Toast.LENGTH_SHORT).show();
                return;
            } else if ("returned".equals(status)) {
                Toast.makeText(this, "订单已退货，无需重复操作", Toast.LENGTH_SHORT).show();
                return;
            } else if (!"pending".equals(status)) {
                Toast.makeText(this, "订单状态异常，无法操作", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 显示退货原因选择对话框 - 修改为正确的列表对话框
        String[] reasons = new String[]{"商品质量问题", "商品与描述不符", "发错商品", "其他原因"};

        Log.d(TAG, "显示退货原因选择对话框，选项数: " + reasons.length);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("申请退货");
        builder.setMessage("请选择退货原因");
        builder.setItems(reasons, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String reason = reasons[which];
                Log.d(TAG, "选择退货原因: " + reason + " (索引: " + which + ")");
                applyReturn(orderId, position, reason);
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG, "用户取消退货");
                dialog.dismiss();
            }
        });

        // 创建并显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();

        Log.d(TAG, "退货对话框已显示");
    }

    private void completeOrder(String orderId, int position) {
        if (StaticVar.cookie == null || StaticVar.cookie.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("order_id", orderId)
                .build();

        HttpUtil.sendOkHttpRequest(COMPLETE_ORDER_URL, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(myBought.this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "确认收货响应: " + responseData);

                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        if (json.getString("status").equals("success")) {
                            Toast.makeText(myBought.this, "确认收货成功！", Toast.LENGTH_SHORT).show();

                            // 更新本地数据状态
                            if (position >= 0 && position < boughtList.size()) {
                                Bought bought = boughtList.get(position);
                                // 这里可以添加状态更新逻辑
                            }

                            // 重新加载订单列表
                            loadOrders();
                        } else {
                            Toast.makeText(myBought.this, "操作失败: " + json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(myBought.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    public void applyReturn(String orderId, int position, String reason) {
        if (StaticVar.cookie == null || StaticVar.cookie.isEmpty()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestBody = new FormBody.Builder()
                .add("cookie", StaticVar.cookie)
                .add("order_id", orderId)
                .add("reason", reason)
                .build();

        HttpUtil.sendOkHttpRequest(RETURN_ORDER_URL, requestBody, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(myBought.this, "网络错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                Log.d(TAG, "退货申请响应: " + responseData);

                runOnUiThread(() -> {
                    try {
                        JSONObject json = new JSONObject(responseData);
                        if (json.getString("status").equals("success")) {
                            Toast.makeText(myBought.this, "退货申请已提交", Toast.LENGTH_SHORT).show();

                            // 重新加载订单列表
                            loadOrders();
                        } else {
                            Toast.makeText(myBought.this, "操作失败: " + json.getString("message"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(myBought.this, "数据解析失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private String getFinalImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            Log.d(TAG, "图片路径为空，使用默认图片");
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
        Log.d(TAG, "onResume");
        // 可以在这里刷新数据，但为了避免重复加载，暂时注释
        // loadOrders();
    }

//    // 添加一个测试方法，确保activity引用正确
//    public void testButtonClick(String message) {
//        Log.d(TAG, "测试方法被调用: " + message);
//        Toast.makeText(this, "测试: " + message, Toast.LENGTH_SHORT).show();
//    }
// 在 myBought.java 中添加
public void applyReturnSimple(String orderId, int position) {
    Log.d(TAG, "简化版退货申请，订单ID: " + orderId);

    // 这里直接调用退货接口，不需要原因参数
    // 你可以根据你的后端接口调整
    applyReturn(orderId, position, "用户申请退货"); // 使用默认原因
}


}