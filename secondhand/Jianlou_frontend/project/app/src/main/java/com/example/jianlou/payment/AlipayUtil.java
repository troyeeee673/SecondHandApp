package com.example.jianlou.payment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.alipay.sdk.app.EnvUtils;

/**
 * 支付宝支付工具类
 */
public class AlipayUtil {

    private Context context;
    private String alipayPackageName = "com.eg.android.AlipayGphone"; // 默认正式版包名
    private PaymentCallback paymentCallback;

    // public修饰符，让外部包可访问
    public interface PaymentCallback {
        void onPaymentSuccess(String result);
        void onPaymentFailure(String errorCode, String errorMsg);
        void onPaymentProcessing(String message);
    }

    public AlipayUtil(Context context) {
        this.context = context;
    }

    /**
     * 设置支付结果回调
     */
    public void setPaymentCallback(PaymentCallback callback) {
        this.paymentCallback = callback;
    }

    /**
     * 发起支付宝支付
     */

    public void pay(String orderString) {
        // 1. 启动子线程进行支付（支付任务是耗时操作）
        new Thread(() -> {
            com.alipay.sdk.app.EnvUtils.setEnv(com.alipay.sdk.app.EnvUtils.EnvEnum.SANDBOX);

            // 2. 创建 PayTask 实例
            com.alipay.sdk.app.PayTask alipay = new com.alipay.sdk.app.PayTask((Activity) context);

            // 3. 调用支付接口（第二个参数表示是否显示 loading）
            // 即使是沙箱版，只要你手机装了“支付宝沙箱版”App，SDK 会自动检测并拉起
            String result = alipay.pay(orderString, true);

            // 4. 处理结果发送回主线程
            ((Activity) context).runOnUiThread(() -> {
                if (paymentCallback != null) {
                    // 解析 resultStatus，9000 代表成功
                    if (result.contains("resultStatus={9000}")) {
                        paymentCallback.onPaymentSuccess(result);
                    } else {
                        paymentCallback.onPaymentFailure("error", result);
                    }
                }
            });
        }).start();
    }
}

