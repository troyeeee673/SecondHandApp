// DeleteRecordRepository.java
package com.example.jianlou.repository;

import com.example.jianlou.entity.DeleteRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeleteRecordRepository extends JpaRepository<DeleteRecord, Long> {
    // 检查商品是否被软删除
    boolean existsByGoodsHash(String goodsHash);

    // 根据商品hash查找删除记录
    DeleteRecord findByGoodsHash(String goodsHash);

    // 恢复商品（删除删除记录）
    void deleteByGoodsHash(String goodsHash);
}