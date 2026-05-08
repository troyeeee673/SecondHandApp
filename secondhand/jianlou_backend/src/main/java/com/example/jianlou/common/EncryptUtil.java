package com.example.jianlou.common;

import org.springframework.stereotype.Component;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 加盐加密
@Component
public class EncryptUtil {
    private static final String SALT = "`1234567890~!@#$%^&*()-=[];',./ZXCVBNSADFYWQET";

    // MD5加密
    public String md5hex(String asciiStr) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(asciiStr.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // 加盐加密
    public String saltedPassword(String password) {
        String hash1 = md5hex(password);
        String hash2 = md5hex(hash1 + SALT);
        return hash2;
    }
}