package com.example.jianlou.repository;

import com.example.jianlou.entity.FollowRecord;
import com.example.jianlou.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowRecordRepository extends JpaRepository<FollowRecord, Long> {
    // 检查关注关系是否存在
    boolean existsByFollowerAccountAndFollowingAccount(String followerAccount, String followingAccount);

    // 获取关注记录
    Optional<FollowRecord> findByFollowerAccountAndFollowingAccount(String followerAccount, String followingAccount);

    // 获取用户的粉丝列表
    List<FollowRecord> findByFollowingAccount(String followingAccount);

    // 获取用户的关注列表
    List<FollowRecord> findByFollowerAccount(String followerAccount);

    // 统计粉丝数
    int countByFollowingAccount(String followingAccount);

    // 统计关注数
    int countByFollowerAccount(String followerAccount);
}