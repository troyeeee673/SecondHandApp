package com.example.jianlou;

import com.example.jianlou.common.EncryptUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class EncryptUtilTest {
    @Autowired
    private EncryptUtil encryptUtil;

    @Test
    public void testSaltedPassword() { // 建议方法名以test开头，更易识别
        // 1. 待测试的明文密码
        String plainPassword = "0000";
        // 2. 数据库中的管理员密文
        String dbPassword = "f0cbd3102ecce3d4f3d0edf0d2cf3b5e";
        // 3. 调用加盐加密
        String encryptedPassword = encryptUtil.saltedPassword(plainPassword);
        // 4. 打印结果，直接对比
        System.out.println("明文密码：" + plainPassword);
        System.out.println("加密后密码：" + encryptedPassword);
        System.out.println("数据库密文：" + dbPassword);
        System.out.println("是否匹配：" + encryptedPassword.equals(dbPassword));
    }
}