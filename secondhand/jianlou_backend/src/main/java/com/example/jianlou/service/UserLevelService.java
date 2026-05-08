package com.example.jianlou.service;

import com.example.jianlou.config.UserLevelConfig;
import com.example.jianlou.entity.Order;
import com.example.jianlou.entity.User;
import com.example.jianlou.repository.OrderRepository;
import com.example.jianlou.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLevelService {

    private final UserRepository userRepository;
    private final UserLevelConfig userLevelConfig;
    private final OrderRepository orderRepository;

    @PostConstruct
    public void initUserLevels() {
        log.info("=== 开始初始化所有用户等级 ===");
        try {
            updateAllUserLevels();
            log.info("用户等级初始化完成，共处理 {} 个用户", userRepository.count());
        } catch (Exception e) {
            log.error("初始化用户等级失败", e);
        }
    }

    /**
     * 计算用户等级分数
     * 公式：总分数 = 活跃度分数 × 活跃度权重 + 信誉度分数 × 信誉度权重
     */
    public int calculateUserScore(User user) {
        int activeScore = user.getActiveScore() != null ? user.getActiveScore() : 0;
        int creditScore = user.getCreditScore() != null ? user.getCreditScore() : 60;
        double successRate = user.getSuccessRate() != null ? user.getSuccessRate() : 1;

        // 调整分数范围
        activeScore = Math.max(0, Math.min(100, activeScore));
        creditScore = Math.max(0, Math.min(100, creditScore));

        double totalScore = activeScore * userLevelConfig.getActiveWeight()
                + creditScore * successRate * 0.01 * userLevelConfig.getCreditWeight();

        return (int) Math.round(totalScore);
    }

    private double calculateActualSuccessRate(String account) {
        try {
            List<Order> allOrders = orderRepository.findBySellerAccount(account);
            if (allOrders.isEmpty()) return 100.0;

            // 只统计已完成的交易（completed和returned）
            List<Order> completedOrders = allOrders.stream()
                    .filter(order -> "completed".equals(order.getStatus()) || "returned".equals(order.getStatus()))
                    .collect(Collectors.toList());

            if (completedOrders.isEmpty()) return 100.0;

            long completedCount = completedOrders.stream()
                    .filter(order -> "completed".equals(order.getStatus()))
                    .count();

            long totalCompleted = completedOrders.size();

            double successRate = (double) completedCount / totalCompleted * 100;
            return Math.round(successRate * 100.0) / 100.0;
        } catch (Exception e) {
            log.error("计算成功率失败: 卖家={}", account, e);
            return 100.0;
        }
    }

    @Transactional
    public void handleTransactionComplete(String buyerAccount, String sellerAccount) {
        try {
            log.info("开始处理交易完成 - 买家: {}, 卖家: {}", buyerAccount, sellerAccount);

            // 1. 先执行原生SQL更新
            userRepository.incrementActiveScore(buyerAccount, Integer.valueOf(5));
            userRepository.incrementTransactionCount(buyerAccount, Integer.valueOf(1));
            userRepository.incrementCreditScore(sellerAccount, Integer.valueOf(5));
            userRepository.incrementTransactionCount(sellerAccount, Integer.valueOf(1));

            // 2. 手动刷新确保更新生效
            userRepository.flush();

            // 3. 计算新的成功率并更新到数据库
            double sellerSuccessRate = calculateActualSuccessRate(sellerAccount);

            // 4. 更新卖家的成功率到数据库（重要！）
            userRepository.updateSuccessRate(sellerAccount, sellerSuccessRate);

            log.info("卖家新成功率: {}%", sellerSuccessRate);

            // 5. 计算并更新等级
            updateUserLevelInternal(buyerAccount);
            updateUserLevelInternal(sellerAccount);

            log.info("交易完成处理完成: 买家={}, 卖家={}", buyerAccount, sellerAccount);

        } catch (Exception e) {
            log.error("处理交易完成失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void handleReviewSubmit(String reviewerAccount) {
        try {
            log.info("开始处理评论提交: 评论者={}", reviewerAccount);

            // 1. 执行原生SQL更新
            userRepository.incrementActiveScore(reviewerAccount, Integer.valueOf(2));
            userRepository.incrementReviewCount(reviewerAccount, Integer.valueOf(1));

            // 2. 手动刷新
            userRepository.flush();

            // 3. 计算并更新等级
            updateUserLevelInternal(reviewerAccount);

            log.info("评论提交处理完成: 评论者={}", reviewerAccount);

        } catch (Exception e) {
            log.error("处理评论提交失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public void handleReturn(String sellerAccount) {
        try {
            log.info("开始处理退货 - 卖家: {}", sellerAccount);

            // 1. 执行原生SQL更新（信誉度扣3分）
            userRepository.incrementCreditScore(sellerAccount, Integer.valueOf(-3));

            // 2. 手动刷新
            userRepository.flush();

            // 3. 计算新的成功率并更新到数据库
            double sellerSuccessRate = calculateActualSuccessRate(sellerAccount);
            userRepository.updateSuccessRate(sellerAccount, sellerSuccessRate);

            log.info("退货处理完成 - 卖家: {}, 新成功率: {}%", sellerAccount, sellerSuccessRate);

            // 4. 计算并更新等级
            updateUserLevelInternal(sellerAccount);

        } catch (Exception e) {
            log.error("处理退货失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 内部方法：只更新等级，不触发全量更新
     */
    private void updateUserLevelInternal(String account) {
        try {
            // 1. 重新获取用户信息（确保数据最新）
            User user = userRepository.findByAccount(account);
            if (user == null) {
                log.warn("用户不存在: {}", account);
                return;
            }

            // 2. 计算分数
            int score = calculateUserScore(user);
            String level = userLevelConfig.getLevelByScore(score);

            // 3. 使用原生SQL更新等级（不触发全量更新）
            userRepository.updateUserLevel(account, level, score);

            log.info("等级更新成功: account={}, level={}, score={}", account, level, score);

        } catch (Exception e) {
            log.error("更新等级失败: account={}, error={}", account, e.getMessage(), e);
            throw e;
        }
    }

    @Transactional
    public UserLevelInfo calculateAndUpdateLevel(String account) {
        try {
            User user = userRepository.findByAccount(account);
            if (user == null) {
                log.warn("用户不存在: {}", account);
                return null;
            }

            // 计算分数和等级
            int score = calculateUserScore(user);
            String level = userLevelConfig.getLevelByScore(score);

            // 使用自定义方法更新等级
            userRepository.updateUserLevel(account, level, score);

            log.info("用户等级更新成功: account={}, level={}, score={}", account, level, score);

            // 重新获取用户信息构建返回对象
            user = userRepository.findByAccount(account);
            return buildUserLevelInfo(user, score, level);

        } catch (Exception e) {
            log.error("计算更新等级失败: account={}, error={}", account, e.getMessage(), e);
            return null;
        }
    }

    @Transactional
    public void updateAllUserLevels() {
        List<User> users = userRepository.findAll();
        log.info("开始批量计算 {} 个用户的等级", users.size());

        int successCount = 0;
        for (User user : users) {
            try {
                int score = calculateUserScore(user);
                String level = userLevelConfig.getLevelByScore(score);
                userRepository.updateUserLevel(user.getAccount(), level, score);
                successCount++;
            } catch (Exception e) {
                log.error("更新用户 {} 等级失败: {}", user.getAccount(), e.getMessage());
            }
        }
        log.info("批量更新完成，成功更新 {} 个用户", successCount);
    }

    @Scheduled(cron = "${user.level.calculate-cron:0 0 2 1 * ?}")
    public void scheduleRecalculateLevels() {
        log.info("开始定时计算用户等级...");
        try {
            updateAllUserLevels();
            log.info("定时计算用户等级完成");
        } catch (Exception e) {
            log.error("定时计算用户等级失败: {}", e.getMessage());
        }
    }

    public UserLevelInfo getUserLevelInfo(String account) {
        User user = userRepository.findByAccount(account);
        if (user == null) return null;

        int score = calculateUserScore(user);
        String level = userLevelConfig.getLevelByScore(score);
        return buildUserLevelInfo(user, score, level);
    }

    private UserLevelInfo buildUserLevelInfo(User user, int score, String level) {
        // 获取下一等级信息
        UserLevelConfig.NextLevelInfo configNextLevel = userLevelConfig.getNextLevelInfo(level, score);

        // 转换为UserLevelInfo.NextLevelInfo
        UserLevelInfo.NextLevelInfo nextLevelInfo = null;
        if (configNextLevel != null) {
            nextLevelInfo = UserLevelInfo.NextLevelInfo.builder()
                    .level(configNextLevel.getLevel())
                    .needScore(configNextLevel.getNeedScore())
                    .totalScore(configNextLevel.getTotalScore())
                    .build();
        }

        return UserLevelInfo.builder()
                .account(user.getAccount())
                .username(user.getUsername())
                .level(level)
                .levelScore(score)
                .activeScore(user.getActiveScore() != null ? user.getActiveScore() : 0)
                .creditScore(user.getCreditScore() != null ? user.getCreditScore() : 60)
                .transactionCount(user.getTransactionCount() != null ? user.getTransactionCount() : 0)
                .reviewCount(user.getReviewCount() != null ? user.getReviewCount() : 0)
                .successRate(user.getSuccessRate() != null ? user.getSuccessRate() : 100.0)
                .lastCalcTime(user.getLastCalcTime())
                .nextLevel(nextLevelInfo)
                .build();
    }

    public List<UserLevelInfo> getLevelRanking(int limit) {
        List<User> users = userRepository.findByOrderByLevelScoreDesc();
        return users.stream()
                .limit(limit)
                .map(user -> {
                    int score = user.getLevelScore() != null ? user.getLevelScore() : 0;
                    String level = user.getLevel() != null ? user.getLevel() : "青铜";
                    return buildUserLevelInfo(user, score, level);
                })
                .collect(Collectors.toList());
    }

    /**
     * 处理商品发布，增加用户活跃度
     */
    @Transactional
    public void handleGoodsPublish(String publisherAccount) {
        try {
            log.info("处理商品发布 - 发布者: {}", publisherAccount);

            // 1. 增加活跃度（发布商品+3分）
            userRepository.incrementActiveScore(publisherAccount, Integer.valueOf(3));

            // 2. 手动刷新
            userRepository.flush();

            // 3. 计算并更新等级
            updateUserLevelInternal(publisherAccount);

            log.info("商品发布处理完成: 发布者={}, 活跃度+3", publisherAccount);
        } catch (Exception e) {
            log.error("处理商品发布失败: {}", e.getMessage(), e);
            // 这里不抛出异常，避免影响商品发布
        }
    }

    public Map<String, Long> getLevelStatistics() {
        List<User> allUsers = userRepository.findAll();
        Map<String, Long> statistics = new HashMap<>();

        // 获取所有等级配置
        List<UserLevelConfig.LevelThreshold> thresholds = userLevelConfig.getThresholds();

        if (thresholds != null) {
            // 初始化所有等级
            for (UserLevelConfig.LevelThreshold threshold : thresholds) {
                statistics.put(threshold.getName(), 0L);
            }
        } else {
            // 如果配置为空，使用默认等级
            String[] defaultLevels = {"青铜", "白银", "黄金", "钻石"};
            for (String level : defaultLevels) {
                statistics.put(level, 0L);
            }
        }

        // 统计各等级用户数
        for (User user : allUsers) {
            String level = user.getLevel() != null ? user.getLevel() : "青铜";
            statistics.put(level, statistics.getOrDefault(level, 0L) + 1);
        }

        return statistics;
    }

    @Data
    @Builder
    public static class UserLevelInfo {
        private String account;
        private String username;
        private String level;
        private Integer levelScore;
        private Integer activeScore;
        private Integer creditScore;
        private Integer transactionCount;
        private Integer reviewCount;
        private Double successRate;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        private Date lastCalcTime;

        // 改为内部类
        private NextLevelInfo nextLevel;

        @Data
        @Builder
        public static class NextLevelInfo {
            private String level;
            private Integer needScore;
            private Integer totalScore;
        }

        public double getProgressPercentage() {
            if (nextLevel == null) return 100.0;

            int currentSegmentStart = nextLevel.getTotalScore() - nextLevel.getNeedScore();
            int progressInSegment = levelScore - currentSegmentStart;

            // 确保不会出现负数或超过100%
            double progress = Math.max(0, Math.min(100,
                    (double) progressInSegment / nextLevel.getNeedScore() * 100));

            return progress;
        }
    }
}