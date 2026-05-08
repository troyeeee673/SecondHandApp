package com.example.jianlou.controller;

import com.example.jianlou.config.UserLevelConfig;
import com.example.jianlou.entity.User;
import com.example.jianlou.repository.UserRepository;
import com.example.jianlou.service.UserLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-level")
@RequiredArgsConstructor
public class UserLevelController {

    private final UserLevelService userLevelService;
    private final UserRepository userRepository;      // 添加这个
    private final UserLevelConfig userLevelConfig;   // 添加这个

    /**
     * 获取用户等级信息
     */
    @GetMapping("/{account}")
    public UserLevelService.UserLevelInfo getUserLevel(@PathVariable String account) {
        return userLevelService.getUserLevelInfo(account);
    }

    // ... 其他原有方法保持不变 ...

    /**
     * 测试接口 - 返回简单的用户等级信息
     */
    @GetMapping("/test/simple/{account}")
    public Map<String, Object> testSimple(@PathVariable String account) {
        Map<String, Object> result = new HashMap<>();

        // 1. 测试用户是否存在
        User user = userRepository.findByAccount(account);
        if (user == null) {
            result.put("error", "用户不存在");
            result.put("account", account);
            return result;
        }

        // 2. 添加基本字段
        result.put("account", user.getAccount());
        result.put("username", user.getUsername());
        result.put("level", user.getLevel());
        result.put("levelScore", user.getLevelScore());
        result.put("activeScore", user.getActiveScore());
        result.put("creditScore", user.getCreditScore());
        result.put("transactionCount", user.getTransactionCount());
        result.put("reviewCount", user.getReviewCount());
        result.put("successRate", user.getSuccessRate());

        // 3. 测试NextLevelInfo
        try {
            UserLevelConfig.NextLevelInfo nextLevel = userLevelConfig.getNextLevelInfo(
                    user.getLevel(),
                    user.getLevelScore() != null ? user.getLevelScore() : 0
            );

            if (nextLevel != null) {
                Map<String, Object> nextLevelMap = new HashMap<>();
                nextLevelMap.put("level", nextLevel.getLevel());
                nextLevelMap.put("needScore", nextLevel.getNeedScore());
                nextLevelMap.put("totalScore", nextLevel.getTotalScore());
                result.put("nextLevel", nextLevelMap);
            } else {
                result.put("nextLevel", null);
                result.put("nextLevelInfo", "没有下一等级信息");
            }
        } catch (Exception e) {
            result.put("nextLevelError", e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 调试接口 - 检查服务层返回的数据
     */
    @GetMapping("/debug/{account}")
    public Map<String, Object> debugUserLevel(@PathVariable String account) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 1. 获取服务层数据
            UserLevelService.UserLevelInfo serviceInfo = userLevelService.getUserLevelInfo(account);
            if (serviceInfo == null) {
                result.put("error", "服务层返回null");
                return result;
            }

            // 2. 记录基本信息
            result.put("account", serviceInfo.getAccount());
            result.put("username", serviceInfo.getUsername());
            result.put("level", serviceInfo.getLevel());
            result.put("levelScore", serviceInfo.getLevelScore());

            // 3. 检查nextLevel字段
            Object nextLevelObj = serviceInfo.getNextLevel();
            result.put("nextLevelExists", nextLevelObj != null);

            if (nextLevelObj != null) {
                result.put("nextLevelClass", nextLevelObj.getClass().getName());
                result.put("nextLevelToString", nextLevelObj.toString());

                // 尝试反射获取字段
                try {
                    Class<?> clazz = nextLevelObj.getClass();
                    if (clazz.getMethod("getLevel") != null) {
                        Object levelValue = clazz.getMethod("getLevel").invoke(nextLevelObj);
                        result.put("reflectedLevel", levelValue);
                    }
                    if (clazz.getMethod("getNeedScore") != null) {
                        Object needScoreValue = clazz.getMethod("getNeedScore").invoke(nextLevelObj);
                        result.put("reflectedNeedScore", needScoreValue);
                    }
                    if (clazz.getMethod("getTotalScore") != null) {
                        Object totalScoreValue = clazz.getMethod("getTotalScore").invoke(nextLevelObj);
                        result.put("reflectedTotalScore", totalScoreValue);
                    }
                } catch (Exception e) {
                    result.put("reflectionError", e.getMessage());
                }
            }

        } catch (Exception e) {
            result.put("exception", e.getMessage());
            e.printStackTrace();
        }

        return result;
    }
}