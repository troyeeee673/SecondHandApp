package com.example.jianlou.service;

import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeAppPayRequest;
import com.alipay.api.response.AlipayTradeAppPayResponse;
import com.example.jianlou.config.AlipayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig; // 注入配置类

    // 注意：这里需要改成能被外网访问的地址，或者用内网穿透
    // 对于手机端测试，可以使用内网穿透工具如ngrok
    private static final String NOTIFY_URL = "http://192.168.60.245:8080/api/alipay/notify";

    public String createAppPayOrder(String outTradeNo, String totalAmount, String subject) {
        try {
            log.info("=== 开始创建支付宝订单 ===");
            log.info("订单号: {}", outTradeNo);
            log.info("金额: {}", totalAmount);
            log.info("标题: {}", subject);
            log.info("回调地址: {}", NOTIFY_URL);

            AlipayTradeAppPayRequest request = new AlipayTradeAppPayRequest();

            // 使用更详细的参数，特别注意要添加seller_id
            String bizContent = String.format(
                    "{" +
                            "\"out_trade_no\":\"%s\"," +
                            "\"total_amount\":\"%s\"," +
                            "\"subject\":\"%s\"," +
                            "\"seller_id\":\"%s\"," + // ✅ 添加卖家PID
                            "\"product_code\":\"QUICK_MSECURITY_PAY\"," +
                            "\"timeout_express\":\"30m\"," +
                            "\"body\":\"%s\"," +
                            "\"enable_pay_channels\":\"balance,moneyFund,debitCardExpress\"" +
                            "}",
                    outTradeNo,
                    totalAmount,
                    subject,
                    AlipayConfig.PID, // ✅ 使用配置中的PID
                    subject
            );

            log.info("业务参数: {}", bizContent);

            request.setBizContent(bizContent);
            request.setNotifyUrl(NOTIFY_URL);

            AlipayTradeAppPayResponse response = alipayClient.sdkExecute(request);

            // 详细日志
            log.info("支付宝响应状态: {}", response.isSuccess());
            log.info("支付宝响应码: {}", response.getCode());
            log.info("支付宝响应消息: {}", response.getMsg());
            log.info("支付宝响应子码: {}", response.getSubCode());
            log.info("支付宝响应子消息: {}", response.getSubMsg());

            String orderString = response.getBody();
            log.info("支付宝返回的orderString长度: {}", orderString.length());
            log.info("支付宝返回的orderString前100字符: {}",
                    orderString.length() > 100 ? orderString.substring(0, 100) : orderString);

            // 输出完整的orderString用于调试
            log.info("=== 完整的orderString ===");
            log.info("{}", orderString);

            if (response.isSuccess()) {
                log.info("✅ 支付宝订单创建成功：{}", outTradeNo);
                return orderString;
            } else {
                log.error("❌ 支付宝订单创建失败");
                log.error("错误码: {}", response.getCode());
                log.error("错误消息: {}", response.getMsg());
                log.error("子错误码: {}", response.getSubCode());
                log.error("子错误消息: {}", response.getSubMsg());

                // 根据错误码给出具体建议
                if ("40004".equals(response.getCode())) {
                    throw new RuntimeException("支付宝参数错误，请检查APP_ID、私钥和网关配置");
                } else if ("40002".equals(response.getCode())) {
                    throw new RuntimeException("支付宝签名错误，请检查密钥配置");
                } else if ("INVALID_PARAMETER".equals(response.getSubCode())) {
                    throw new RuntimeException("支付宝参数无效，请检查业务参数格式");
                }

                throw new RuntimeException("支付宝支付创建失败：" + response.getMsg() + "，" + response.getSubMsg());
            }

        } catch (Exception e) {
            log.error("支付宝支付异常", e);
            throw new RuntimeException("支付宝支付异常：" + e.getMessage());
        }
    }
}