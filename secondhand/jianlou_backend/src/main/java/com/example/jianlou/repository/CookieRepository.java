package com.example.jianlou.repository;

import com.example.jianlou.entity.Cookie;
import org.springframework.data.jpa.repository.JpaRepository;

// 泛型第二个参数改为Long（对应新增的id主键类型）
public interface CookieRepository extends JpaRepository<Cookie, Long> {
    // 保留原有查询方法
    Cookie findByAccount(String account);
    Cookie findByCookie(String cookie);
}