package com.example.jianlou.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class GoodsStatsDTO {
    // 商品总价（由money字段求和）
    private BigDecimal totalPrice;
    // 分类数量（去重后的分类数）
    private long categoryCount;
    // 所有分类列表（去重）
    private List<String> categoryList;
}