package com.example.jianlou.repository;

import com.example.jianlou.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {

    User findByAccount(String account);

    @Modifying
    @Query("UPDATE User u SET u.userStatus = :status WHERE u.account = :account")
    void updateUserStatusByAccount(@Param("account") String account, @Param("status") String status);


    // 等级相关方法
    List<User> findByLevel(String level);
    List<User> findByOrderByLevelScoreDesc();
    List<User> findByOrderByActiveScoreDesc();
    List<User> findByOrderByCreditScoreDesc();

    // ========== 修复：删除重复定义，只保留一个版本 ==========

    // 更新用户等级
    @Modifying
    @Transactional
    @Query(value = "UPDATE user SET level = :level, level_score = :score, last_calc_time = NOW() WHERE account = :account",
            nativeQuery = true)
    void updateUserLevel(@Param("account") String account,
                         @Param("level") String level,
                         @Param("score") Integer score);

    // 增加活跃度 - 只保留这一个
    @Modifying
    @Transactional
    @Query(value = "UPDATE user SET active_score = COALESCE(active_score, 0) + :increment WHERE account = :account",
            nativeQuery = true)
    void incrementActiveScore(@Param("account") String account, @Param("increment") Integer increment);

    // 增加信誉度 - 只保留这一个
    @Modifying
    @Transactional
    @Query(value = "UPDATE user SET credit_score = COALESCE(credit_score, 60) + :increment WHERE account = :account",
            nativeQuery = true)
    void incrementCreditScore(@Param("account") String account, @Param("increment") Integer increment);

    // 增加交易次数
    @Modifying
    @Transactional
    @Query(value = "UPDATE user SET transaction_count = COALESCE(transaction_count, 0) + :increment WHERE account = :account",
            nativeQuery = true)
    void incrementTransactionCount(@Param("account") String account, @Param("increment") Integer increment);

    // 增加评论次数
    @Modifying
    @Transactional
    @Query(value = "UPDATE user SET review_count = COALESCE(review_count, 0) + :increment WHERE account = :account",
            nativeQuery = true)
    void incrementReviewCount(@Param("account") String account, @Param("increment") Integer increment);

    // 在UserRepository中添加
    @Modifying
    @Transactional
    @Query(value = "UPDATE user SET success_rate = :rate WHERE account = :account",
            nativeQuery = true)
    void updateSuccessRate(@Param("account") String account, @Param("rate") Double rate);
}