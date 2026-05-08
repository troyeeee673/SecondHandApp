package com.example.jianlou.my;

import android.net.Uri;

public class Sold {
    private Uri photoID;       // 商品图片
    private String content;    // 商品名称
    private String money;      // 价格
    private String orderId;    // 订单ID
    private String buyerName;  // 买家姓名
    private String buyerAccount; // 买家账号（新增）
    private String createTime; // 出售时间
    private String goodsId;    // 商品ID
    private String orderStatus; // 订单状态（新增）

    public Sold(Uri photoID, String content, String money, String orderId,
                String buyerName, String buyerAccount, String createTime,
                String goodsId, String orderStatus) {
        this.photoID = photoID;
        this.content = content;
        this.money = money;
        this.orderId = orderId;
        this.buyerName = buyerName;
        this.buyerAccount = buyerAccount;
        this.createTime = createTime;
        this.goodsId = goodsId;
        this.orderStatus = orderStatus;
    }

    // 原有构造函数（为了向后兼容）
    public Sold(Uri photoID, String content, String money, String orderId,
                String buyerName, String createTime, String goodsId) {
        this(photoID, content, money, orderId, buyerName, "", createTime, goodsId, "pending");
    }

    // Getter 方法
    public Uri getPhotoID() {
        return photoID;
    }

    public String getContent() {
        return content;
    }

    public String getMoney() {
        return money;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public String getBuyerAccount() {
        return buyerAccount;
    }

    public String getCreateTime() {
        return createTime;
    }

    public String getGoodsId() {
        return goodsId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    // Setter 方法
    public void setPhotoID(Uri photoID) {
        this.photoID = photoID;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMoney(String money) {
        this.money = money;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public void setBuyerAccount(String buyerAccount) {
        this.buyerAccount = buyerAccount;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public void setGoodsId(String goodsId) {
        this.goodsId = goodsId;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    // 辅助方法：获取完整的买家信息
    public String getFullBuyerInfo() {
        if (buyerAccount != null && !buyerAccount.isEmpty()) {
            return "买家：" + buyerName + " (" + buyerAccount + ")";
        } else {
            return "买家：" + buyerName;
        }
    }

    // 辅助方法：获取完整的出售时间信息
    public String getFullCreateTime() {
        return "出售时间：" + createTime;
    }
}