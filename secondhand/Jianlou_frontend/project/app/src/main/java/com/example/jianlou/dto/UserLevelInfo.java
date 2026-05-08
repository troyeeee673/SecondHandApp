package com.example.jianlou.dto;

import java.util.Date;

public class UserLevelInfo {
    private String account;
    private String username;
    private String level;
    private Integer levelScore;
    private Integer activeScore;
    private Integer creditScore;
    private Integer transactionCount;
    private Integer reviewCount;
    private Double successRate;
    private Date lastCalcTime;
    private NextLevelInfo nextLevel;  // 注意这个类的字段要匹配后端

    // 无参构造
    public UserLevelInfo() {}

    // getters 和 setters
    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getLevelScore() {
        return levelScore;
    }

    public void setLevelScore(Integer levelScore) {
        this.levelScore = levelScore;
    }

    public Integer getActiveScore() {
        return activeScore;
    }

    public void setActiveScore(Integer activeScore) {
        this.activeScore = activeScore;
    }

    public Integer getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(Integer creditScore) {
        this.creditScore = creditScore;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Date getLastCalcTime() {
        return lastCalcTime;
    }

    public void setLastCalcTime(Date lastCalcTime) {
        this.lastCalcTime = lastCalcTime;
    }

    public NextLevelInfo getNextLevel() {
        return nextLevel;
    }

    public void setNextLevel(NextLevelInfo nextLevel) {
        this.nextLevel = nextLevel;
    }

    // 内部类：下一等级信息
    public static class NextLevelInfo {
        private String level;
        private Integer needScore;
        private Integer totalScore;  // 后端返回的是totalScore

        public NextLevelInfo() {}

        public NextLevelInfo(String level, Integer needScore, Integer totalScore) {
            this.level = level;
            this.needScore = needScore;
            this.totalScore = totalScore;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public Integer getNeedScore() {
            return needScore;
        }

        public void setNeedScore(Integer needScore) {
            this.needScore = needScore;
        }

        public Integer getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(Integer totalScore) {
            this.totalScore = totalScore;
        }
    }
}