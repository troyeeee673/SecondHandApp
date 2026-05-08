package com.example.jianlou.index;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.jianlou.BuildConfig;
import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.payment.AlipayUtil;
import com.example.jianlou.staticVar.StaticVar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 商品购买详情页
 * 处理商品展示、支付方式选择、支付宝/微信支付逻辑
 * 适配：正式版支付宝开启沙箱模式
 */
public class PurchaseDetailActivity extends AppCompatActivity {

    // ===================== 常量定义 =====================
    private static final String TAG = "PurchaseDetailActivity";//日志标签
    private static final int PAYMENT_WECHAT = 1; //支付方式 - 微信
    private static final int PAYMENT_ALIPAY = 2;  //支付方式 - 支付宝
    private static final Pattern PRICE_PATTERN = Pattern.compile("[0-9.]+"); //价格匹配正则（提取数字和小数点）
    private String BACKEND_BASE_URL;//后端基础地址
    /**
     * 状态常量
     */
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";
    private static final String YUAN_SYMBOL = "¥";

    // ===================== 成员变量 =====================
    // 商品信息
    private String goodsID;
    private String productName;
    private String price;
    private String sellerAccount;
    private String sellerName;

    // 支付相关
    private int selectedPayment = 0; // 默认未选择
    private double finalPaymentAmount = 0.00; // 最终支付金额

    // UI组件
    private TextView tvProductName;
    private TextView tvPrice;
    private TextView tvSeller;
    private TextView tvProductAmount;
    private TextView tvTotalAmount;
    private TextView tvBottomTotal;
    private ImageView ivWechatSelected;
    private ImageView ivAlipaySelected;
    private LinearLayout layoutWechat;
    private LinearLayout layoutAlipay;
    private LinearLayout ll_loading;
    private Button btnConfirmPayment;

    // 对话框实例（用于生命周期管理）
    private AlertDialog alipayEnvDialog;
    private AlertDialog sandboxGuideDialog;

    // ===================== 生命周期方法 =====================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_detail);
        // 初始化配置
        initConfig();
        // 获取传递的商品信息
        getIntentData();
        // 初始化UI
        initViews();
        // 设置支付选择事件
        setupPaymentSelection();
        // 设置确认支付按钮事件
        setupConfirmButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放资源
        releaseResources();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "PurchaseDetailActivity 销毁完成");
        }
    }

    // ===================== 初始化方法 =====================
    private void initConfig() {
        try {
            BACKEND_BASE_URL = getString(R.string.backend_base_url);
        } catch (Exception e) {
            // 备用地址（防止资源文件未配置）
            BACKEND_BASE_URL = "http://192.168.93.1:8080";
            Log.w(TAG, "未从资源文件获取到后端地址，使用备用地址：" + BACKEND_BASE_URL);
        }
    }

     //===================获取Intent传递的商品数据==================
    private void getIntentData() {
        Intent intent = getIntent();
        goodsID = intent.getStringExtra("goodsID");
        productName = intent.getStringExtra("productName");
        price = intent.getStringExtra("price");
        sellerAccount = intent.getStringExtra("sellerAccount");
        sellerName = intent.getStringExtra("sellerName");

        // 底部显示需要支付的价格
        finalPaymentAmount = parsePrice(price);

    }

     //===============================初始化UI组件=========================
    private void initViews() {
        // 绑定UI组件
        tvProductName = findViewById(R.id.tv_product_name);
        tvPrice = findViewById(R.id.tv_price);
        tvSeller = findViewById(R.id.tv_seller);
        tvProductAmount = findViewById(R.id.tv_product_amount);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvBottomTotal = findViewById(R.id.tv_bottom_total);
        ivWechatSelected = findViewById(R.id.iv_wechat_selected);
        ivAlipaySelected = findViewById(R.id.iv_alipay_selected);
        layoutWechat = findViewById(R.id.layout_wechat);
        layoutAlipay = findViewById(R.id.layout_alipay);
        ll_loading = findViewById(R.id.ll_loading);
        btnConfirmPayment = findViewById(R.id.btn_confirm_payment);

        // 设置商品信息
        setProductInfo();

        // 初始状态
        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText("请选择支付方式");
        updatePaymentUI("请选择支付方式", false);
    }


     //============================设置商品信息到UI===================
    private void setProductInfo() {
        // 商品名称
        if (productName != null) {
            tvProductName.setText(productName);
        }

        // 价格
        String formattedPrice = getFormattedPrice(finalPaymentAmount);
        tvPrice.setText(formattedPrice);
        tvProductAmount.setText(formattedPrice);
        tvTotalAmount.setText(formattedPrice);

        // 卖家信息
        if (sellerName != null && !sellerName.isEmpty()) {
            tvSeller.setText("卖家：" + sellerName);
        }
    }

    // ===================== 设置支付方式选择事件 =====================
    private void setupPaymentSelection() {
        // 微信支付选择
        layoutWechat.setOnClickListener(v -> {
            selectedPayment = PAYMENT_WECHAT;
            ivWechatSelected.setVisibility(View.VISIBLE);
            ivAlipaySelected.setVisibility(View.GONE);
            onPaymentSelected();
        });

        // 支付宝支付选择
        layoutAlipay.setOnClickListener(v -> {
            selectedPayment = PAYMENT_ALIPAY;
            ivWechatSelected.setVisibility(View.GONE);
            ivAlipaySelected.setVisibility(View.VISIBLE);
            onPaymentSelected();
        });
    }

    /**
     * 设置确认支付按钮事件
     */
    private void setupConfirmButton() {
        btnConfirmPayment.setOnClickListener(v -> handleConfirmPayment());
    }

    // ===================== 支付方式选择完成后的处理业务逻辑 =====================
    private void onPaymentSelected() {
        String formattedPrice = getFormattedPrice(finalPaymentAmount);
        updatePaymentUI(formattedPrice, false);

        // 启用支付按钮
        btnConfirmPayment.setEnabled(true);
        btnConfirmPayment.setText("确认支付");
    }

     //==========================处理确认支付逻辑=======================
    private void handleConfirmPayment() {
        // 前置校验
        if (!validatePreConditions()) {
            return;
        }

        showLoadingState();//显示正在支付

        if (selectedPayment == PAYMENT_ALIPAY) {
            // 支付宝
            updatePaymentUI("支付处理中...", false);
            showAlipayEnvironmentCheckDialog(); //给出用支付宝沙箱的提示
        } else if (selectedPayment == PAYMENT_WECHAT) {
            // 微信支付逻辑：因为没集成，我们直接通过“创建订单”来模拟支付成功
            updatePaymentUI("模拟微信支付中...", false);

            // 这里延迟 1 秒，让用户感觉有个“处理”过程，体验更好
            btnConfirmPayment.postDelayed(() -> {
                // 直接调用创建订单接口，不传支付宝交易号
                createOrder(null);
            }, 1000);
        }
    }

    //验证支付前置条件
    private boolean validatePreConditions() {
        // 1. 检查支付方式
        if (selectedPayment == 0) {
            Toast.makeText(this, "请先选择支付方式", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 2. 检查用户登录状态
        synchronized (StaticVar.class) {
            if (StaticVar.cookie == null || StaticVar.cookie.isEmpty()) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        // 3. 检查卖家信息
        if (sellerAccount == null || sellerAccount.isEmpty()) {
            Toast.makeText(this, "卖家信息获取失败", Toast.LENGTH_SHORT).show();
            return false;
        }

        // 4. 检查用户信息并校验自买自卖
        synchronized (StaticVar.class) {
            StaticVar.loadUserInfo(this);
            String currentUserAccount = StaticVar.user_name;

            // 打印到 Logcat 观察
            Log.d(TAG, "校验开始 -> 卖家:" + sellerAccount + " | 当前用户:" + currentUserAccount);

            if (currentUserAccount != null && sellerAccount != null) {
                if (currentUserAccount.contains(sellerAccount) || sellerAccount.contains(currentUserAccount)) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "不能购买自己发布的商品", Toast.LENGTH_SHORT).show();
                        hideLoadingState();
                    });
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * 请求支付宝支付,第一次请求
     */
    private void requestAlipayPayment() {
        // 生成唯一订单号
        String outTradeNo = "ALIPAY_" + goodsID + "_" + System.currentTimeMillis();
        String subject = productName != null ? productName : "二手商品交易";

        // 构建请求参数
        RequestBody requestBody = new FormBody.Builder()
                .add("outTradeNo", outTradeNo)
                .add("totalAmount", String.format("%.2f", finalPaymentAmount))
                .add("subject", subject)
                .add("sellerAccount", sellerAccount) // 卖家账号
                .add("buyerAccount", StaticVar.user_name) // 传入当前登录的用户名
                .build();

        // 支付宝支付接口地址
        String alipayUrl = BACKEND_BASE_URL + "/api/alipay/create";

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "请求支付宝支付: " + alipayUrl);
            Log.d(TAG, "支付参数: outTradeNo=" + outTradeNo +
                    ", totalAmount=" + finalPaymentAmount + ", subject=" + subject);
        }

        // 发送请求
        HttpUtil.sendOkHttpRequest(alipayUrl, requestBody, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "支付宝支付请求失败", e);
                runOnUiThread(() -> {
                    hideLoadingState();
                    updatePaymentUI("网络请求失败，请重试", true);
                    Toast.makeText(PurchaseDetailActivity.this,
                            "支付请求失败，请检查网络", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleAlipayResponse(response, outTradeNo);
            }
        });
    }

    /**
     * 处理支付宝支付响应
     *
     * @param response   响应对象
     * @param outTradeNo 本地订单号
     */
    private void handleAlipayResponse(Response response, String outTradeNo) throws IOException {
        String responseData = response.body() != null ? response.body().string() : "";
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "支付宝支付响应原始数据: " + responseData);
            Log.d(TAG, "支付宝支付响应码: " + response.code());
        }

        runOnUiThread(() -> hideLoadingState());

        try {
            JSONObject json = new JSONObject(responseData);
            String status = json.optString("status", "");
            String message = json.optString("message", "");
            String orderString = json.optString("order_string", json.optString("data", ""));

            if (STATUS_SUCCESS.equals(status) && !orderString.isEmpty()) {
                // 获取到订单字符串，启动支付
                runOnUiThread(() -> startAlipayPayment(orderString, outTradeNo));
            } else {
                // 支付创建失败
                String errorMessage = message.isEmpty() ? "支付创建失败" : message;
                runOnUiThread(() -> {
                    updatePaymentUI("支付宝支付失败：" + errorMessage, true);
                    Toast.makeText(PurchaseDetailActivity.this,
                            "支付宝支付失败：" + errorMessage, Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "支付响应解析失败", e);
            runOnUiThread(() -> {
                updatePaymentUI("支付响应解析失败", true);
                Toast.makeText(PurchaseDetailActivity.this,
                        "支付响应解析失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * 启动支付宝支付（适配正式版沙箱模式）
     *
     * @param orderString 订单字符串
     * @param outTradeNo  本地订单号
     */
    private void startAlipayPayment(String orderString, String outTradeNo) {
        // 验证订单字符串
        if (orderString == null || orderString.isEmpty()) {
            Log.e(TAG, "订单字符串为空");
            runOnUiThread(() -> {
                hideLoadingState();
                updatePaymentUI("支付信息错误，请重试", true);
                Toast.makeText(this, "支付信息错误，请重试", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        try {
            //自动复制订单信息到剪贴板
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("支付宝订单信息", orderString);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "订单信息已复制到剪贴板", Toast.LENGTH_LONG).show();

            // 创建支付宝工具实例
            AlipayUtil alipayUtil = new AlipayUtil(this);

            // 设置支付结果回调
            alipayUtil.setPaymentCallback(new AlipayUtil.PaymentCallback() {
                @Override
                public void onPaymentSuccess(String result) {
                    // 1. 虽然解析了流水号，但我们只把它发给后端存着
                    String alipayTradeNo = parseTradeNoFromResult(result);

                    runOnUiThread(() -> {
                        updatePaymentUI("支付成功！", false);
                        // 2.createOrder 之后，跳转时传递 outTradeNo（本地订单号）
                        createOrder(alipayTradeNo);
                    });
                }

                @Override
                public void onPaymentFailure(String errorCode, String errorMsg) {
                    Log.e(TAG, "支付宝支付失败回调: " + errorCode + " - " + errorMsg);
                    runOnUiThread(() -> handleAlipayFailure(errorCode, errorMsg));
                }

                @Override
                public void onPaymentProcessing(String message) {
                    Log.d(TAG, "支付宝支付处理中回调: " + message);
                    runOnUiThread(() -> updatePaymentUI("支付处理中...", false));
                }
            });

            // 执行支付
            alipayUtil.pay(orderString);

        } catch (Exception e) {
            Log.e(TAG, "支付宝支付启动失败", e);
            runOnUiThread(() -> {
                hideLoadingState();
                updatePaymentUI("支付宝支付失败", true);
                Toast.makeText(this,
                        "支付宝支付启动失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * 解析支付宝返回的交易号
     *
     * @param result 支付宝返回结果
     * @return 交易号
     */
    private String parseTradeNoFromResult(String result) {
        if (result == null || result.isEmpty()) {
            return "";
        }

        // 尝试从result字段解析
        Pattern pattern = Pattern.compile("trade_no[=:\\\"']([A-Za-z0-9]+)");
        Matcher matcher = pattern.matcher(result);
        if (matcher.find()) {
            return matcher.group(1);
        }

        // 或者从整个字符串中查找
        String[] parts = result.split("&");
        for (String part : parts) {
            if (part.contains("trade_no")) {
                String[] keyValue = part.split("=");
                if (keyValue.length > 1) {
                    return keyValue[1];
                }
            }
        }

        Log.w(TAG, "未找到trade_no，使用时间戳生成");
        return "TRADE_" + System.currentTimeMillis();
    }

    /**
     * 创建订单（带支付宝支付成功返回的交易号），第二次请求
     *
     * @param alipayTradeNo 支付宝交易号
     */
    private void createOrder(String alipayTradeNo) {
        Map<String, String> params = new HashMap<>();
        synchronized (StaticVar.class) {
            params.put("cookie", StaticVar.cookie);
        }
        params.put("goods_id", goodsID);
        params.put("seller_account", sellerAccount);

        // 支付宝支付时添加额外参数
        if (alipayTradeNo != null) {
            params.put("payment_method", "alipay");
            params.put("alipay_trade_no", alipayTradeNo);
        } else {
            params.put("payment_method", "wechat");
        }

        // 发送订单请求
        sendOrderRequest(params, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "创建订单失败", e);
                runOnUiThread(() -> {
                    hideLoadingState();
                    Toast.makeText(PurchaseDetailActivity.this,
                            "创建订单失败，请检查网络", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                handleOrderResponse(response, alipayTradeNo);
            }
        });
    }

    // ===================== 第二次请求响应处理 =====================
    /**
     * 处理订单创建响应
     *
     * @param response      响应对象
     * @param alipayTradeNo 支付宝交易号
     */
    private void handleOrderResponse(Response response, String alipayTradeNo) throws IOException {
        runOnUiThread(() -> hideLoadingState());

        if (response.isSuccessful() && response.body() != null) {
            try {
                String responseData = response.body().string();
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "订单创建响应: " + responseData);
                }

                JSONObject jsonObject = new JSONObject(responseData);
                String status = jsonObject.optString("status", STATUS_ERROR);
                String message = jsonObject.optString("message", "");
                String orderId = jsonObject.optString("order_id", "");

                if (STATUS_SUCCESS.equals(status)) {

                    String finalIdToShow = jsonObject.optString("order_id", "");

                    runOnUiThread(() -> {
                        navigateToSuccess(finalIdToShow);
                    });
                } else {
                    // 订单创建失败
                    runOnUiThread(() -> {
                        String tip = message.isEmpty() ? "创建订单失败" : message;
                        Toast.makeText(PurchaseDetailActivity.this, tip, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (JSONException e) {
                Log.e(TAG, "订单数据解析失败", e);
                runOnUiThread(() ->
                        Toast.makeText(PurchaseDetailActivity.this,
                                "订单数据解析失败", Toast.LENGTH_SHORT).show()
                );
            }
        } else {
            Log.e(TAG, "订单创建失败，状态码: " + response.code());
            runOnUiThread(() ->
                    Toast.makeText(PurchaseDetailActivity.this,
                            "服务器错误：" + response.code(), Toast.LENGTH_SHORT).show()
            );
        }
    }


    /**
     * 跳转到购买成功页面
     *
     * @param orderId 订单号
     */
    private void navigateToSuccess(String orderId) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "跳转到成功页面，orderId: " + orderId);
        }

        Intent intent = new Intent(PurchaseDetailActivity.this, BuySuccessActivity.class);
        intent.putExtra("order_amount", finalPaymentAmount);

        // 商品名称截断
        if (productName != null) {
            String shortName = productName.length() > 20 ?
                    productName.substring(0, 20) + "..." : productName;
            intent.putExtra("product_name", shortName);
        }

        intent.putExtra("goods_id", goodsID);
        intent.putExtra("order_id", orderId);
        intent.putExtra("payment_method", selectedPayment == PAYMENT_ALIPAY ? "alipay" : "wechat");

        startActivity(intent);
        finish();
    }


    /**
     * 处理支付宝支付失败
     *
     * @param errorCode 错误码
     * @param errorMsg  错误信息
     */
    private void handleAlipayFailure(String errorCode, String errorMsg) {
        hideLoadingState();

        String message;
        if ("10".equals(errorCode)) {
            message = "支付宝版本不匹配，请确认已登录沙箱账号";
            updatePaymentUI("版本不匹配", true);
            showAlipaySandboxInstallGuide();
        } else if ("6001".equals(errorCode)) {
            message = "用户取消支付";
            updatePaymentUI("支付已取消", true);
        } else if ("6002".equals(errorCode)) {
            message = "网络连接错误";
            updatePaymentUI("网络连接错误", true);
        } else if ("4000".equals(errorCode)) {
            message = "订单支付失败";
            updatePaymentUI("支付失败", true);
        } else {
            message = "支付失败: " + errorMsg;
            updatePaymentUI("支付失败", true);
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // ===================== UI辅助方法 =====================
    /**
     * 显示加载状态
     */
    private void showLoadingState() {
        ll_loading.setVisibility(View.VISIBLE);
        btnConfirmPayment.setEnabled(false);
        btnConfirmPayment.setText("正在处理...");
    }

    /**
     * 隐藏加载状态
     */
    private void hideLoadingState() {
        ll_loading.setVisibility(View.GONE);
        btnConfirmPayment.setEnabled(true);
        btnConfirmPayment.setText("确认支付");
    }

    /**
     * 更新支付UI显示
     *
     * @param text     显示文本
     * @param isError  是否为错误状态
     */
    private void updatePaymentUI(String text, boolean isError) {
        tvBottomTotal.setText(text);

        if (isError) {
            // 错误状态：红色字体
            tvBottomTotal.setTextColor(Color.parseColor("#FF3B30"));
            tvBottomTotal.setTextSize(14);
            tvBottomTotal.setTypeface(null, Typeface.NORMAL);
        } else {
            // 正常状态
            if (text.equals("请选择支付方式")) {
                // 提示信息：灰色
                tvBottomTotal.setTextColor(Color.parseColor("#666666"));
                tvBottomTotal.setTextSize(14);
                tvBottomTotal.setTypeface(null, Typeface.NORMAL);
            } else if (text.startsWith(YUAN_SYMBOL)) {
                // 金额：红色粗体
                tvBottomTotal.setTextColor(Color.parseColor("#e53935"));
                tvBottomTotal.setTextSize(18);
                tvBottomTotal.setTypeface(null, Typeface.BOLD);
            } else if (text.contains("支付处理中") || text.contains("支付成功")) {
                // 处理中/成功：绿色粗体
                tvBottomTotal.setTextColor(Color.parseColor("#4CAF50"));
                tvBottomTotal.setTextSize(18);
                tvBottomTotal.setTypeface(null, Typeface.BOLD);
            }
        }
    }



    // ===================== 显示支付宝环境检测对话框 =====================
    private void showAlipayEnvironmentCheckDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("支付宝沙箱支付提示")
                .setMessage("请确保你已登录支付宝沙箱测试账号，否则支付会失败！")
                .setPositiveButton("我已确认", (dialog, which) -> {
                    dialog.dismiss();
                    requestAlipayPayment();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    hideLoadingState();
                })
                .setCancelable(false);

        alipayEnvDialog = builder.create();
        alipayEnvDialog.show();
    }

    /**
     * 显示支付宝沙箱版安装指引
     */
    private void showAlipaySandboxInstallGuide() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("支付宝沙箱支付指引")
                .setMessage("当前未检测到支付宝沙箱环境，请按以下步骤操作：\\n\\n" +
                        "1. 打开支付宝，确保登录沙箱测试账号（左上角有沙箱标识）\\n" +
                        "2. 若未登录，请前往支付宝开放平台获取沙箱账号\\n" +
                        "3. 支付密码：111111")
                .setPositiveButton("下载教程", (dialog, which) ->
                        openBrowserSafely("https://opensupport.alipay.com/support/knowledge/20070/201602278647"))
                .setNeutralButton("复制信息", (dialog, which) -> copySandboxInfo())
                .setNegativeButton("取消", null)
                .setCancelable(false);

        sandboxGuideDialog = builder.create();
        sandboxGuideDialog.show();
    }

    // ===================== 工具方法 =====================
    /**
     * 解析价格字符串
     *
     * @param price 价格字符串
     * @return 解析后的价格
     */
    private double parsePrice(String price) {
        if (price == null) {
            return 0.00;
        }

        // 提取数字和小数点
        Matcher matcher = PRICE_PATTERN.matcher(price);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group());
            } catch (NumberFormatException e) {
                Log.e(TAG, "价格解析失败", e);
            }
        }

        return 0.00;
    }

    /**
     * 格式化价格显示
     *
     * @param price 价格
     * @return 格式化后的价格字符串
     */
    private String getFormattedPrice(double price) {
        return YUAN_SYMBOL + String.format(" %.2f", price);
    }


    /**
     * 安全地打开浏览器
     *
     * @param url 网址
     */
    private void openBrowserSafely(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "未找到浏览器应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "打开浏览器失败", e);
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制沙箱信息到剪贴板
     */
    private void copySandboxInfo() {
        String info = "支付宝沙箱版安装信息：\\n" +
                "1. 访问支付宝开放平台沙箱环境\\n" +
                "2. 下载沙箱版APK或在正式版中登录沙箱账号\\n" +
                "3. 安装后使用测试账号登录\\n" +
                "4. 支付密码：111111";

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("支付宝沙箱教程", info);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    /**
     * 发送订单请求
     *
     * @param params   请求参数
     * @param callback 回调
     */
    private void sendOrderRequest(Map<String, String> params, Callback callback) {
        FormBody.Builder builder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }

        String createOrderUrl = BACKEND_BASE_URL + "/orders/create/";
        HttpUtil.sendOkHttpRequest(createOrderUrl, builder.build(), callback);
    }

    /**
     * 释放资源（防止内存泄漏）
     */
    private void releaseResources() {
        // 关闭对话框
        if (alipayEnvDialog != null && alipayEnvDialog.isShowing()) {
            alipayEnvDialog.dismiss();
        }
        if (sandboxGuideDialog != null && sandboxGuideDialog.isShowing()) {
            sandboxGuideDialog.dismiss();
        }

        // 回收图片资源
        if (ivWechatSelected != null) {
            ivWechatSelected.setImageDrawable(null);
        }
        if (ivAlipaySelected != null) {
            ivAlipaySelected.setImageDrawable(null);
        }

        // 清空UI引用
        tvProductName = null;
        tvPrice = null;
        tvSeller = null;
        tvProductAmount = null;
        tvTotalAmount = null;
        tvBottomTotal = null;
        ivWechatSelected = null;
        ivAlipaySelected = null;
        layoutWechat = null;
        layoutAlipay = null;
        ll_loading = null;
        btnConfirmPayment = null;
    }
}