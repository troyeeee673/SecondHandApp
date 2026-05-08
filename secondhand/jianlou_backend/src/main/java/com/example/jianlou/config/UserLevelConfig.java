package com.example.jianlou.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "user.level")
@Data
public class UserLevelConfig {

    // 直接声明Logger
    private static final Logger log = LoggerFactory.getLogger(UserLevelConfig.class);

    private String calculateCron;
    private double creditWeight;
    private double activeWeight;
    private List<LevelThreshold> thresholds;

    @PostConstruct
    public void init() {
        log.info("=== UserLevelConfig 初始化 ===");
        log.info("calculateCron: {}", calculateCron);
        log.info("creditWeight: {}", creditWeight);
        log.info("activeWeight: {}", activeWeight);

        if (thresholds != null) {
            log.info("配置的thresholds数量: {}", thresholds.size());
            for (int i = 0; i < thresholds.size(); i++) {
                LevelThreshold t = thresholds.get(i);
                log.info("  threshold[{}]: name={}, min={}, max={}",
                        i, t.getName(), t.getMin(), t.getMax());
            }
        } else {
            log.warn("thresholds 为 null! 请检查application.yml配置");
            // 使用默认配置
            thresholds = new ArrayList<>();
            thresholds.add(new LevelThreshold(0, 60, "青铜"));
            thresholds.add(new LevelThreshold(61, 80, "白银"));
            thresholds.add(new LevelThreshold(81, 95, "黄金"));
            thresholds.add(new LevelThreshold(96, 100, "钻石"));
            log.info("已使用默认thresholds配置");
        }
    }

    @Data
    public static class LevelThreshold {
        private int min;
        private int max;
        private String name;

        public LevelThreshold() {
        }

        public LevelThreshold(int min, int max, String name) {
            this.min = min;
            this.max = max;
            this.name = name;
        }
    }

    /**
     * 根据分数获取等级名称
     */
    public String getLevelByScore(int score) {
        score = Math.max(0, Math.min(100, score));

        if (thresholds != null) {
            for (LevelThreshold threshold : thresholds) {
                if (score >= threshold.getMin() && score <= threshold.getMax()) {
                    return threshold.getName();
                }
            }
        }

        // 默认配置（当配置文件没有配置时）
        if (score >= 96) return "钻石";
        if (score >= 81) return "黄金";
        if (score >= 61) return "白银";
        return "青铜";
    }

    /**
     * 获取下一等级信息
     */
    public NextLevelInfo getNextLevelInfo(String currentLevel, int currentScore) {
        // 添加日志
        if (log.isDebugEnabled()) {
            log.debug("获取下一等级信息: currentLevel={}, currentScore={}", currentLevel, currentScore);
        }

        if (thresholds == null || thresholds.size() < 2) {
            log.warn("thresholds配置问题: thresholds={}, size={}", thresholds,
                    thresholds != null ? thresholds.size() : 0);
            return getDefaultNextLevel(currentLevel, currentScore);
        }

        // 按min分数排序
        thresholds.sort((a, b) -> Integer.compare(a.getMin(), b.getMin()));

        if (log.isDebugEnabled()) {
            log.debug("排序后的thresholds:");
            for (int i = 0; i < thresholds.size(); i++) {
                LevelThreshold t = thresholds.get(i);
                log.debug("  [{}] {}: min={}, max={}", i, t.getName(), t.getMin(), t.getMax());
            }
        }

        // 找到当前等级的位置
        int currentIndex = -1;
        for (int i = 0; i < thresholds.size(); i++) {
            if (thresholds.get(i).getName().equals(currentLevel)) {
                currentIndex = i;
                break;
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("当前等级索引: {}", currentIndex);
        }

        // 如果已经是最高等级或没找到
        if (currentIndex == -1) {
            log.warn("未找到当前等级: {}", currentLevel);
            return getDefaultNextLevel(currentLevel, currentScore);
        }

        if (currentIndex == thresholds.size() - 1) {
            log.info("当前是最高等级: {}", currentLevel);
            return null;
        }

        LevelThreshold nextThreshold = thresholds.get(currentIndex + 1);

        int needScore = Math.max(0, nextThreshold.getMin() - currentScore);
        int totalScore = nextThreshold.getMin();

        if (log.isDebugEnabled()) {
            log.debug("下一等级信息: level={}, needScore={}, totalScore={}",
                    nextThreshold.getName(), needScore, totalScore);
        }

        return new NextLevelInfo(nextThreshold.getName(), needScore, totalScore);
    }

    /**
     * 默认下一等级逻辑（当配置失效时使用）
     */
    private NextLevelInfo getDefaultNextLevel(String currentLevel, int currentScore) {
        log.info("使用默认下一等级逻辑: currentLevel={}, currentScore={}", currentLevel, currentScore);

        switch (currentLevel) {
            case "青铜":
                int needForSilver = Math.max(0, 61 - currentScore);
                return new NextLevelInfo("白银", needForSilver, 61);
            case "白银":
                int needForGold = Math.max(0, 81 - currentScore);
                return new NextLevelInfo("黄金", needForGold, 81);
            case "黄金":
                int needForDiamond = Math.max(0, 96 - currentScore);
                return new NextLevelInfo("钻石", needForDiamond, 96);
            case "钻石":
                return null;
            default:
                // 未知等级，默认青铜->白银
                int needForDefault = Math.max(0, 61 - currentScore);
                return new NextLevelInfo("白银", needForDefault, 61);
        }
    }

    @Data
    public static class NextLevelInfo {
        private String level;
        private int needScore;
        private int totalScore;

        public NextLevelInfo(String level, int needScore, int totalScore) {
            this.level = level;
            this.needScore = needScore;
            this.totalScore = totalScore;
        }
    }
}