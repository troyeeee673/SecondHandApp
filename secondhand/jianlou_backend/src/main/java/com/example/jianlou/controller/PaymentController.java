package com.example.jianlou.controller;

import com.example.jianlou.service.AlipayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api")
@Slf4j
public class PaymentController {

    @Autowired
    private AlipayService alipayService;

    /**
     * 创建支付宝支付，目的返回支付包可以识别的交易号
     * POST /api/alipay/create
     */
    @PostMapping("/alipay/create")
    public Map<String, Object> createAlipayPayment(
            @RequestParam("outTradeNo") String outTradeNo,
            @RequestParam("totalAmount") String totalAmount,
            @RequestParam("subject") String subject,
            @RequestParam("sellerAccount") String sellerAccount, // 接收卖家账号
            @RequestParam("buyerAccount") String buyerAccount   // 接收买家账号
    ) {

        Map<String, Object> result = new HashMap<>();

        try {
            log.info("创建支付宝订单校验: 买家={}, 卖家={}, 订单号={}", buyerAccount, sellerAccount, outTradeNo);

            // 1. 防止自买自卖
            if (buyerAccount != null && sellerAccount != null) {
                // 只要买家账号里包含了卖家账号（手机号），就视为同一个人
                if (buyerAccount.contains(sellerAccount) || sellerAccount.contains(buyerAccount)) {
                    result.put("status", "error");
                    result.put("message", "抱歉，您不能购买自己发布的商品");
                    return result;
                }
            }

            // 2. 原有的参数校验
            if (outTradeNo == null || outTradeNo.isEmpty() || totalAmount == null || totalAmount.isEmpty()) {
                result.put("status", "error");
                result.put("message", "订单参数缺失");
                return result;
            }

            // 3. 原有的金额校验
            BigDecimal amount = new BigDecimal(totalAmount);//高精度金额对象
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                result.put("status", "error");
                result.put("message", "金额异常");
                return result;
            }

            // 4. 只有校验通过，才调用支付宝生成支付串
            String orderString = alipayService.createAppPayOrder(
                    outTradeNo,
                    amount.toPlainString(),
                    subject
            );

            result.put("status", "success");
            result.put("order_string", orderString);

        } catch (Exception e) {
            log.error("创建支付失败", e);
            result.put("status", "error");
            result.put("message", "系统繁忙，请稍后再试");
        }

        return result;
    }
}
