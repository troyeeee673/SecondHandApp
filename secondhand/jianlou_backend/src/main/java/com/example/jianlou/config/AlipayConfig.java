package com.example.jianlou.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Slf4j
@Configuration
public class AlipayConfig {


    // ===================== 支付宝配置常量 =====================
    public static final String APP_ID = "9021000158665790";
    public static final String GATEWAY_URL = "https://openapi-sandbox.di.alipaydev.com/gateway.do"; // 沙箱环境


    public static final String PID = "2088721090743834"; // ✅ 沙箱商户PID

    public static final String CHARSET = "UTF-8";
    public static final String SIGN_TYPE = "RSA2";
    public static final String FORMAT = "JSON";

    // ===================== 密钥处理 =====================
    // 应用私钥
    private static final String RAW_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCHFnXEFKizDVi4oqE52RSwnQPA3PeAa3YWonVgY7OVbFho+vwgxU2Ra80AzXaIMTMEhHmzFP1vqrikqgPlcieC6EVQyFO/8VWbdIBIqxn/91jdr/Im70S78Dau6QMGSCPZncwOYO7jV+gNpPbvL3pFHrZODQOg3YKk/2unc9A4iakjaIK7wYRBr8FyWJG/FZkh2Q8G+J2ug0p034YpCwMMhV7Xwe/63RKa4L9jMhhwDu9aUz8Uglg96jRxvANrFJRwqr8drrfavE+U+CQIPgSVCx9GtrFEr8CHng0W7qFpC8vfrvE4/bz9bi34FQpSJavheTv+FUIOJzhvIyWRCbNDAgMBAAECggEANHoqJ5Dq9BukMp2iyiklW3KziZGNaErWpfZYaKhylAJx2d5eYZCkEHDzQ3ONr1yFeCTKbiIiTeXhMr6FIvYUmUsH2ljoB/GFkw1P5Grrf78ju4LlK/DZH7UyWQJV1XPi0V/2bEOFqbV1hQ9nigVEl2NV6L4vgadrYBOFS5i2868dinVygtQ+4YvSLXkztuwt485XlL5yhTKyNwcDsJm7a02JbnuI+QUllfMtosKF2CF1XHosE32LU0kpRj9mgQWf67LBxqfXwPvbpxX5WiRSiWXS5ez3v3WNwFg6xifqAVwS8V1870t3js+5o1zcwRiT1C3QWqb8fXgfcRN1P4kL6QKBgQDKwDyNvnaOIWOThjlD6GBLUG3wFpuC9jMibzKqEbDqfBwVNyzI3MttTM8uGEaG9HTn4tHTqUpuoRlxsSWrMuxJx2CEuI5FvnCEfDq0Zci4Cp6QUVff4u8JU0WeKqmDDcA9BEehtRk3M7y3Njwyx6wC8Sav5/YxXJiP18W2n6BKlwKBgQCqkPQ4Zd8KOAqUsgnfyAIhBX4xKI2J2wrPbU2/qyRHgyiiWIGHuVfLyTL8WlqF+6JCc8y/vJH9kv8cRXE4/j9Ezkl1iWFWzUZTrfINLmHTL2OYmqbtpz4Vg9LJN2WbHGsErn3sEdSWGEsV5JAwzY+44QWRYYNgU+AJpNHxHCoONQKBgQCeP40D1TUTmlvuOUVZC3adUhl82yhl/2PJ7KDOvSAo5NXGeVorLKpalSjgAXKKwyK8Gv1LB0dhMbx6UJnmFcj3jPZ4oNPq+8k2nv/y7b7MZv18BwFfbfDEhoo/+Wx1LbZt6Xk/bepQe8E//sTdnZtUcISRp6swu+UX4IIhGFYz4QKBgGAzyctVietVmIItuOwC/1BorLhFSy+J4BsGZ2fHu3qqO2z1xnkqzJ4gKSW0QIJ5XxdkAQPT8/O1yTXE5QtkO4MvZrdoeQJgXV4tKezNUFewztfvwl8giR/Rbj4XhLNr3+CQGm3f0KunoBIFDF66UmnQYTeSbGsUY7SMxezcGcxRAoGBAKWB2mcBCIVwE5Z/yIAsPPrB53xe6iU5dt6yNcNLa7/4RQavcoaSenBxRE/Rcbhe1oEWRnKF3VmRrsY9qfqnAfM5uAWR4A/JUFSdTT6r6KsH3ARlruuDdyeMm3pmNiDfoElHSPKECzPjVWmqRkXeyMYx35gw7wXR5NySOXxqzrln";

    // 支付宝公钥
    private static final String RAW_PUBLIC_KEY =
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhBDThOVcB9tgchS83QSZ0n6P2cpxdF6OQwCsYRRh3A+2p+vdl2XUx+x4S7CK6+ZuuOZJaEI8Q4etocbsXheL9ENBAkjgJ5gKJORQzcgBNtcq5bCDGjYUTH04tbXGk6R+Cf3gcO8NnUORIqgMzvCSpkbB9K2zTfdIhBQVxrdLpTjQM0+MWvvNhlnWz4h8AcQwkM2Jd3eGY+jZlfgF6DOpechlhtRA679fsmS1KBT1Fq+Z4NV+EoZynvKzqI6HMJXyw+qrs79Bjk89ZFIix6PLgrVYh0P7FHEFjn9XSc5LHyYbJHYJdEr3SIOHuNJkXqMIjQ688wZlQwaUjojt22FG8QIDAQAB";
    // 清理后的密钥（纯Base64，无换行符和PEM标识符）
    public static final String APP_PRIVATE_KEY;
    public static final String ALIPAY_PUBLIC_KEY;

    static {
        // 清理私钥：移除换行符、空格和PEM标识符
        APP_PRIVATE_KEY = cleanKey(RAW_PRIVATE_KEY);

        // 公钥通常不需要PEM标识符，直接清理换行符和空格
        ALIPAY_PUBLIC_KEY = cleanKey(RAW_PUBLIC_KEY);
    }

    /**
     * 清理密钥：移除所有换行符、空格和PEM标识符
     * @param key 原始密钥字符串
     * @return 清理后的纯Base64密钥
     */
    private static String cleanKey(String key) {
        if (key == null) {
            return null;
        }

        // 移除PEM标识符
        String cleaned = key
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "");

        // 移除所有空白字符（换行符、空格、制表符等）
        cleaned = cleaned.replaceAll("\\s", "");

        return cleaned.trim();
    }

    @PostConstruct
    public void init() {
        log.info("=== 支付宝沙箱配置初始化 ===");
        log.info("沙箱APP_ID: {}", APP_ID);
        log.info("沙箱网关: {}", GATEWAY_URL);
        log.info("沙箱商户PID: {}", PID);

        // 环境验证
        if (APP_ID.equals("9021000158665790")) {
            log.info("✅ APP_ID正确（沙箱环境）");
        } else {
            log.error("❌ APP_ID错误！应该是9021000158665790");
        }

        if (GATEWAY_URL.contains("sandbox")) {
            log.info("✅ 网关地址正确（沙箱环境）");
        } else {
            log.error("❌ 网关地址错误！应该包含sandbox");
        }

        // 检查密钥是否为沙箱密钥（沙箱密钥通常较短）
        if (APP_PRIVATE_KEY.length() > 1500) {
            log.warn("⚠️ 私钥较长，可能是正式环境密钥，沙箱密钥通常较短");
        }
    }

    @Bean
    public AlipayClient alipayClient() {
        try {
            log.info("正在创建支付宝沙箱客户端...");

            // 验证配置
            if (!APP_ID.equals("9021000158665790")) {
                log.error("❌ APP_ID不正确！沙箱APP_ID应为9021000158665790");
                throw new RuntimeException("APP_ID配置错误");
            }

            if (APP_PRIVATE_KEY.equals("你的沙箱应用私钥")) {
                log.error("❌ 请配置沙箱应用私钥！");
                throw new RuntimeException("未配置沙箱密钥");
            }

            AlipayClient client = new DefaultAlipayClient(
                    GATEWAY_URL,
                    APP_ID,
                    APP_PRIVATE_KEY,
                    FORMAT,
                    CHARSET,
                    ALIPAY_PUBLIC_KEY,
                    SIGN_TYPE
            );

            log.info("✅ 支付宝沙箱客户端创建成功");
            log.info("    APP_ID: {}", APP_ID);
            log.info("    网关: {}", GATEWAY_URL);

            return client;

        } catch (Exception e) {
            log.error("创建支付宝客户端失败: {}", e.getMessage(), e);
            throw new RuntimeException("支付宝客户端初始化失败", e);
        }
    }

}