package com.example.jianlou.repository;

import com.example.jianlou.entity.Goods;
import com.example.jianlou.entity.GoodsImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GoodsImageRepository extends JpaRepository<GoodsImage, Long> {
    // 根据商品查询图片
    List<GoodsImage> findByGoods(Goods goods);
}