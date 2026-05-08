package com.example.jianlou.service;

import com.example.jianlou.entity.Goods;
import com.example.jianlou.entity.User;
import com.example.jianlou.common.EncryptUtil;
import com.example.jianlou.repository.GoodsRepository;
import com.example.jianlou.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GoodsRepository goodsRepository;
    @Autowired
    private EncryptUtil encryptUtil;


    //管理员登录校验
    public User checkAdminLogin(String account, String password) {
        User admin = userRepository.findByAccount(account);
        if (admin == null) {
            System.out.println("错误：未查询到该账号的用户！");
            return null;
        }

        if (!"admin".equalsIgnoreCase(admin.getUserType())) {
            System.out.println("错误：用户类型不是admin！");
            return null;
        }

        String encryptPwd = encryptUtil.saltedPassword(password);
        System.out.println("输入密码加密后：" + encryptPwd);

        if (admin.getPassword().equals(encryptPwd)) {
            System.out.println("登录成功：密码匹配！");
            return admin;
        } else {
            System.out.println("错误：密码不匹配！");
            return null;
        }
    }

    // ====================== 用户管理相关 ======================
    //获取所有的用户
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    //更新用户状态
    @Transactional
    public void updateUserStatus(String account, String status) {
        userRepository.updateUserStatusByAccount(account, status);
    }

    //数据大屏：商品分类
    public Map<String, Long> countGoodsByClassify() {
        List<Goods> allGoods = goodsRepository.findAll(); //获取数据库中所有商品
        return allGoods.stream()
                .collect(Collectors.groupingBy(Goods::getClassify, Collectors.counting())); //返回一个 Map<String, Long> 类型的结果
    }

    //数据大屏：管理员和用户分类
    public Map<String, Long> countUserByType() {
        List<User> allUsers = userRepository.findAll();
        return allUsers.stream()
                .collect(Collectors.groupingBy(User::getUserType, Collectors.counting()));
    }

}