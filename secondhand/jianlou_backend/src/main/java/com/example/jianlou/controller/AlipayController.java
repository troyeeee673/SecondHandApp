package com.example.jianlou.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.example.jianlou.config.AlipayConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alipay")
@Slf4j
public class AlipayController {

    /**
     * 支付宝异步通知回调
     */
    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        try {
            // 将请求参数转换为Map
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();

            for (String name : requestParams.keySet()) {
                String[] values = requestParams.get(name);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                params.put(name, valueStr);
            }

            log.info("支付宝异步通知参数：{}", params);

            // 验证签名
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    AlipayConfig.ALIPAY_PUBLIC_KEY,
                    AlipayConfig.CHARSET,
                    AlipayConfig.SIGN_TYPE
            );

            if (!signVerified) {
                log.error("支付宝异步通知签名验证失败");
                return "failure";
            }

            // 处理业务逻辑
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no");

            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 支付成功，更新订单状态
                log.info("订单支付成功：{}", outTradeNo);
                // TODO: 更新订单状态为已支付
            }

            return "success"; // 必须返回success，否则支付宝会重复通知

        } catch (AlipayApiException e) {
            log.error("支付宝异步通知处理异常", e);
            return "failure";
        }
    }
}