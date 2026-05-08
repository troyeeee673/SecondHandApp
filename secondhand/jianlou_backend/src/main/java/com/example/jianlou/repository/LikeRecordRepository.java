package com.example.jianlou.repository;

import com.example.jianlou.entity.LikeRecord;
import com.example.jianlou.entity.User;
import com.example.jianlou.entity.Goods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRecordRepository extends JpaRepository<LikeRecord, Long> {
    // 根据用户和商品查找点赞记录
    Optional<LikeRecord> findByUserAccountAndGoodsHash(String userAccount, String goodsHash);

    // 统计商品的点赞数
    int countByGoodsHash(String goodsHash);

    // 检查用户是否点赞过某个商品
    boolean existsByUserAccountAndGoodsHash(String userAccount, String goodsHash);

    // 获取用户的所有点赞记录
    List<LikeRecord> findByUserAccount(String userAccount);

    // 获取商品的所有点赞记录
    List<LikeRecord> findByGoodsHash(String goodsHash);

    // 统计用户收到的总赞数（通过商品）
    @Query("SELECT COUNT(l) FROM LikeRecord l WHERE l.goods.owner.account = :userAccount")
    int countLikesReceivedByUser(@Param("userAccount") String userAccount);
}