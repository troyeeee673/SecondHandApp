package com.example.jianlou.repository;

import com.example.jianlou.entity.Order;
import com.example.jianlou.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    // 根据买家查找订单（基本查询）
    List<Order> findByBuyer(User buyer);

    // 根据卖家查找订单（基本查询）
    List<Order> findBySeller(User seller);

    // 根据订单ID查找
    Order findByOrderId(String orderId);

    // 根据买家账号查找订单（使用 JOIN FETCH 解决懒加载）
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.goods g " +
            "LEFT JOIN FETCH o.buyer b " +
            "LEFT JOIN FETCH o.seller s " +
            "WHERE o.buyer.account = :account " +
            "ORDER BY o.createTime DESC")
    List<Order> findByBuyerAccount(@Param("account") String account);

    // 根据卖家账号查找订单
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.goods g " +
            "LEFT JOIN FETCH o.buyer b " +
            "LEFT JOIN FETCH o.seller s " +
            "WHERE o.seller.account = :account " +
            "ORDER BY o.createTime DESC")
    List<Order> findBySellerAccount(@Param("account") String account);

    // 检查订单是否已存在
    boolean existsByOrderId(String orderId);


}