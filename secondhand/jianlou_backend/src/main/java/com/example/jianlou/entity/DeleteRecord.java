// DeleteRecord.java
package com.example.jianlou.entity;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "delete_record")
public class DeleteRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "goods_hash", nullable = false)
    private String goodsHash;

    @Column(name = "deleted_by", nullable = false)
    private String deletedBy;

    @Column(name = "delete_time", nullable = false)
    private Date deleteTime;

    // 构造函数
    public DeleteRecord() {
        this.deleteTime = new Date();
    }

    public DeleteRecord(String goodsHash, String deletedBy) {
        this.goodsHash = goodsHash;
        this.deletedBy = deletedBy;
        this.deleteTime = new Date();
    }

    // getter和setter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getGoodsHash() { return goodsHash; }
    public void setGoodsHash(String goodsHash) { this.goodsHash = goodsHash; }

    public String getDeletedBy() { return deletedBy; }
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

    public Date getDeleteTime() { return deleteTime; }
    public void setDeleteTime(Date deleteTime) { this.deleteTime = deleteTime; }
}