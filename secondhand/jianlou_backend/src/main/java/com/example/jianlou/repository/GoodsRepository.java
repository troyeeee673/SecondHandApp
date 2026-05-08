package com.example.jianlou.repository;

import com.example.jianlou.entity.Goods;
import com.example.jianlou.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface GoodsRepository extends JpaRepository<Goods, String> {

    @Query("SELECT g FROM Goods g WHERE g.classify LIKE %:classify% ORDER BY g.editDate DESC")
    List<Goods> findByClassifyContainingOrderByEditDateDesc(@Param("classify") String classify);

    @Query("SELECT g FROM Goods g WHERE g.content LIKE %:keyword% ORDER BY g.editDate DESC")
    List<Goods> findByContentContainingOrderByEditDateDesc(@Param("keyword") String keyword);


    List<Goods> findByOwnerOrderByEditDateDesc(User user);


    //根据审核状态查询
    List<Goods> findByAuditStatusOrderByEditDateDesc(String auditStatus);

    //统计不同审核状态的数量
    @Query("SELECT COUNT(g) FROM Goods g WHERE g.auditStatus = :status")
    Long countByAuditStatus(@Param("status") String status);

    // 根据审核状态和分类查询
    @Query("SELECT g FROM Goods g WHERE g.auditStatus = :status AND g.classify LIKE %:classify% ORDER BY g.editDate DESC")
    List<Goods> findByAuditStatusAndClassifyContainingOrderByEditDateDesc(
            @Param("status") String status,
            @Param("classify") String classify);

    //根据发布者账号查询
    @Query("SELECT g FROM Goods g WHERE g.owner.account = :account ORDER BY g.editDate DESC")
    List<Goods> findByOwnerAccountOrderByEditDateDesc(@Param("account") String account);

    //组合搜索：审核状态 + 内容
    @Query("SELECT g FROM Goods g WHERE g.auditStatus = :status AND g.content LIKE %:keyword% ORDER BY g.editDate DESC")
    List<Goods> findByAuditStatusAndContentContainingOrderByEditDateDesc(
            @Param("status") String status,
            @Param("keyword") String keyword);

    //更新商品审核状态
    @Modifying
    @Transactional
    @Query("UPDATE Goods g SET g.auditStatus = :status WHERE g.hash = :hash")
    void updateAuditStatus(@Param("hash") String hash, @Param("status") String status);

    //更新商品审核状态和拒绝理由
    @Modifying
    @Transactional
    @Query("UPDATE Goods g SET g.auditStatus = :status, g.rejectReason = :reason WHERE g.hash = :hash")
    void updateAuditStatusWithReason(@Param("hash") String hash,
                                     @Param("status") String status,
                                     @Param("reason") String reason);

    //获取所有商品并按时间排序（用于全部商品展示）
    @Query("SELECT g FROM Goods g ORDER BY g.editDate DESC")
    List<Goods> findAllByOrderByEditDateDesc();


    //YHT新增
    // 新增：根据状态查询商品
    List<Goods> findByStatusOrderByEditDateDesc(String status);

    // 新增：搜索时考虑状态
    List<Goods> findByContentContainingAndStatusOrderByEditDateDesc(String content, String status);

    // 新增：按分类搜索时考虑状态
    List<Goods> findByClassifyContainingAndStatusOrderByEditDateDesc(String classify, String status);



    // ========== 新增商品统计相关方法 ==========
    // 1. 增加商品销售数量
    @Modifying
    @Transactional
    @Query("UPDATE Goods g SET g.soldNumber = g.soldNumber + 1 WHERE g.hash = :hash")
    void incrementSoldNumber(@Param("hash") String hash);

    // 2. 增加商品退货数量
    @Modifying
    @Transactional
    @Query("UPDATE Goods g SET g.returnNumber = g.returnNumber + 1 WHERE g.hash = :hash")
    void incrementReturnNumber(@Param("hash") String hash);

    // 3. 增加商品总数量
    @Modifying
    @Transactional
    @Query("UPDATE Goods g SET g.totalNumber = g.totalNumber + 1 WHERE g.hash = :hash")
    void incrementTotalNumber(@Param("hash") String hash);

    // 4. 根据销售数量排序查询
    @Query("SELECT g FROM Goods g WHERE g.status = :status ORDER BY g.soldNumber DESC")
    List<Goods> findByStatusOrderBySoldNumberDesc(@Param("status") String status);

    // 5. 查询用户的商品统计数据
    @Query("SELECT COALESCE(SUM(g.soldNumber), 0) as totalSold, " +
            "COALESCE(SUM(g.returnNumber), 0) as totalReturn, " +
            "COALESCE(SUM(g.totalNumber), 0) as totalGoods " +
            "FROM Goods g WHERE g.owner.account = :account")
    Object[] getUserGoodsStats(@Param("account") String account);


    //LY新增——关于价格排序（价格升序、价格降序、价格范围）
    // 1. 价格升序
    @Query("SELECT g FROM Goods g WHERE g.content LIKE %:keyword% AND g.status = :status ORDER BY CAST(g.money AS double) ASC")
    List<Goods> findByContentContainingAndStatusOrderByMoneyAsc(
            @Param("keyword") String keyword,
            @Param("status") String status);

    // 2. 价格降序
    @Query("SELECT g FROM Goods g WHERE g.content LIKE %:keyword% AND g.status = :status ORDER BY CAST(g.money AS double) DESC")
    List<Goods> findByContentContainingAndStatusOrderByMoneyDesc(
            @Param("keyword") String keyword,
            @Param("status") String status);

    // 3. 根据价格范围搜索（按价格升序）
    @Query("SELECT g FROM Goods g WHERE g.content LIKE %:keyword% AND g.status = :status AND CAST(g.money AS double) >= :minPrice AND CAST(g.money AS double) <= :maxPrice ORDER BY CAST(g.money AS double) ASC")
    List<Goods> findByContentAndPriceRangeOrderByPriceAsc(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("minPrice") double minPrice,
            @Param("maxPrice") double maxPrice);

    // 4. 根据价格范围搜索（按价格降序）
    @Query("SELECT g FROM Goods g WHERE g.content LIKE %:keyword% AND g.status = :status AND CAST(g.money AS double) >= :minPrice AND CAST(g.money AS double) <= :maxPrice ORDER BY CAST(g.money AS double) DESC")
    List<Goods> findByContentAndPriceRangeOrderByPriceDesc(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("minPrice") double minPrice,
            @Param("maxPrice") double maxPrice);

    // 5. 分类搜索+价格范围（按价格升序）
    @Query("SELECT g FROM Goods g WHERE g.classify LIKE %:classify% AND g.status = :status AND CAST(g.money AS double) >= :minPrice AND CAST(g.money AS double) <= :maxPrice ORDER BY CAST(g.money AS double) ASC")
    List<Goods> findByClassifyAndPriceRangeOrderByPriceAsc(
            @Param("classify") String classify,
            @Param("status") String status,
            @Param("minPrice") double minPrice,
            @Param("maxPrice") double maxPrice);

    // 6. 分类搜索+价格范围（按价格降序）
    @Query("SELECT g FROM Goods g WHERE g.classify LIKE %:classify% AND g.status = :status AND CAST(g.money AS double) >= :minPrice AND CAST(g.money AS double) <= :maxPrice ORDER BY CAST(g.money AS double) DESC")
    List<Goods> findByClassifyAndPriceRangeOrderByPriceDesc(
            @Param("classify") String classify,
            @Param("status") String status,
            @Param("minPrice") double minPrice,
            @Param("maxPrice") double maxPrice);

    // 分类搜索+价格升序（无价格范围）
    @Query("SELECT g FROM Goods g WHERE g.classify LIKE %:classify% AND g.status = :status ORDER BY CAST(g.money AS double) ASC")
    List<Goods> findByClassifyContainingAndStatusOrderByMoneyAsc(
            @Param("classify") String classify,
            @Param("status") String status);

    // 分类搜索+价格降序（无价格范围）
    @Query("SELECT g FROM Goods g WHERE g.classify LIKE %:classify% AND g.status = :status ORDER BY CAST(g.money AS double) DESC")
    List<Goods> findByClassifyContainingAndStatusOrderByMoneyDesc(
            @Param("classify") String classify,
            @Param("status") String status);
    //LY

}