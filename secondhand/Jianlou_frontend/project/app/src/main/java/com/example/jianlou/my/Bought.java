package com.example.jianlou.my;

import android.net.Uri;

public class Bought {
    private Uri photoID; // 商品图片
    private String content; // 商品名称
    private String money; // 商品价格
    private String orderId; // 订单ID
    private String sellerName; // 卖家名称
    private String sellerAccount; // 卖家账号（新增）
    private String createTime; // 创建时间
    private String goodsId; // 商品ID
    private String orderStatus; // 订单状态（新增）

    // 构造函数
    public Bought(Uri photoID, String content, String money, String orderId,
                  String sellerName, String sellerAccount, String createTime,
                  String goodsId, String orderStatus) {
        this.photoID = photoID;
        this.content = content;
        this.money = money;
        this.orderId = orderId;
        this.sellerName = sellerName;
        this.sellerAccount = sellerAccount;
        this.createTime = createTime;
        this.goodsId = goodsId;
        this.orderStatus = orderStatus;
    }

    // 原有构造函数（为了向后兼容）
    public Bought(Uri photoID, String content, String money, String orderId,
                  String sellerName, String createTime, String goodsId) {
        this(photoID, content, money, orderId, sellerName, "", createTime, goodsId, "pending");
    }

    // Getters and Setters
    public Uri getPhotoID() {
        return photoID;
    }

    public void setPhotoID(Uri photoID) {
        this.photoID = photoID;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMoney() {
        return money;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public void setSellerName(String sellerName) {
        this.sellerName = sellerName;
    }

    public String getSellerAccount() {
        return sellerAccount;
    }

    public void setSellerAccount(String sellerAccount) {
        this.sellerAccount = sellerAccount;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    // 辅助方法：获取完整的卖家信息
    public String getFullSellerInfo() {
        if (sellerAccount != null && !sellerAccount.isEmpty()) {
            return "卖家：" + sellerName + " (" + sellerAccount + ")";
        } else {
            return "卖家：" + sellerName;
        }
    }

    // 辅助方法：获取完整的购买时间信息
    public String getFullCreateTime() {
        return "购买时间：" + createTime;
    }

    // 判断是否显示确认收货按钮
    public boolean shouldShowConfirmButton() {
        // pending 状态显示确认收货按钮
        return "pending".equals(orderStatus);
    }

    // 判断是否显示退货按钮
    public boolean shouldShowReturnButton() {
        // pending 状态也显示退货按钮
        return "pending".equals(orderStatus);
    }
}