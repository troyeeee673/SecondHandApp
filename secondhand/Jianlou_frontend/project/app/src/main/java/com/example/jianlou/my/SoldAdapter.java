package com.example.jianlou.my;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.R;
import com.example.jianlou.index.good_detail;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SoldAdapter extends RecyclerView.Adapter<SoldAdapter.ViewHolder> {
    private List<Sold> mSoldList;
    private static final String TAG = "SoldAdapter";

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView content, money, buyer, time, orderId, status; // 新增 status 和 orderId

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.sold_photo);
            content = itemView.findViewById(R.id.sold_content);
            money = itemView.findViewById(R.id.sold_money);
            buyer = itemView.findViewById(R.id.sold_buyer);
            time = itemView.findViewById(R.id.sold_time);
            orderId = itemView.findViewById(R.id.sold_order_id);
            status = itemView.findViewById(R.id.sold_status);

            Log.d(TAG, "ViewHolder创建 - 找到控件: " +
                    "photo=" + (photo != null) +
                    ", content=" + (content != null) +
                    ", money=" + (money != null) +
                    ", buyer=" + (buyer != null) +
                    ", time=" + (time != null) +
                    ", orderId=" + (orderId != null) +
                    ", status=" + (status != null));
        }
    }

    public SoldAdapter(List<Sold> soldList) {
        if (soldList == null) {
            this.mSoldList = new ArrayList<>();
        } else {
            this.mSoldList = soldList;
        }
        Log.d(TAG, "适配器创建，初始数据大小: " + mSoldList.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "创建ViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sold_recycler_good, parent, false);
        final ViewHolder holder = new ViewHolder(view);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int adapterPosition = holder.getAdapterPosition();

                if (adapterPosition != RecyclerView.NO_POSITION &&
                        mSoldList != null &&
                        adapterPosition < mSoldList.size()) {

                    Sold clickedSold = mSoldList.get(adapterPosition);
                    Log.d(TAG, "点击已售出订单: " + clickedSold.getContent());

                    if (clickedSold.getGoodsId() != null && !clickedSold.getGoodsId().isEmpty()) {
                        Intent intent = new Intent(v.getContext(), good_detail.class);
                        intent.putExtra("goodsID", clickedSold.getGoodsId());
                        v.getContext().startActivity(intent);
                    } else {
                        Log.w(TAG, "商品ID为空，无法跳转");
                    }
                }
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        int adapterPosition = holder.getAdapterPosition();

        if (adapterPosition == RecyclerView.NO_POSITION) {
            Log.w(TAG, "无效的adapter position");
            return;
        }

        if (mSoldList == null || adapterPosition < 0 || adapterPosition >= mSoldList.size()) {
            Log.e(TAG, "位置越界: " + adapterPosition);
            return;
        }

        Sold sold = mSoldList.get(adapterPosition);
        Log.d(TAG, "绑定位置 " + adapterPosition + ": " + sold.getContent() +
                ", 状态: " + sold.getOrderStatus());

        final int finalPosition = adapterPosition;

        // 绑定商品数据
        if (holder.photo != null && sold.getPhotoID() != null) {
            Picasso.get()
                    .load(sold.getPhotoID())
                    .placeholder(R.mipmap.loading)
                    .error(R.mipmap.ic_launcher)
                    .transform(new RoundTransform(10, 10))
                    .into(holder.photo, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "图片加载成功，位置: " + finalPosition);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "图片加载失败: " + e.getMessage());
                        }
                    });
        } else {
            if (holder.photo != null) {
                holder.photo.setImageResource(R.mipmap.ic_launcher);
            }
        }

        // 设置文本内容
        if (holder.content != null) {
            holder.content.setText(sold.getContent());
        }

        if (holder.money != null) {
            holder.money.setText(sold.getMoney());
        }

        // 设置买家信息 - 修改为完整的显示方式
        if (holder.buyer != null) {
            String buyerInfo = sold.getFullBuyerInfo();
            holder.buyer.setText(buyerInfo);
            Log.d(TAG, "买家信息: " + buyerInfo);
        }

        // 设置出售时间 - 修改为完整的显示方式
        if (holder.time != null) {
            String timeInfo = sold.getFullCreateTime();
            holder.time.setText(timeInfo);
            Log.d(TAG, "出售时间: " + timeInfo);
        }

        // 设置订单号
        if (holder.orderId != null) {
            holder.orderId.setText("订单号：" + sold.getOrderId());
        }

        // 设置订单状态显示
        if (holder.status != null) {
            String orderStatus = sold.getOrderStatus();
            String statusText;
            int backgroundColor;

            switch (orderStatus) {
                case "pending":
                    statusText = "等待买家确认";
                    backgroundColor = 0xFF2196F3; // 蓝色
                    break;
                case "completed":
                    statusText = "已完成";
                    backgroundColor = 0xFF4CAF50; // 绿色
                    break;
                case "returned":
                    statusText = "已退货";
                    backgroundColor = 0xFFF44336; // 红色
                    break;
                default:
                    statusText = orderStatus;
                    backgroundColor = 0xFF757575; // 灰色
                    break;
            }

            holder.status.setText("状态: " + statusText);
            holder.status.setBackgroundColor(backgroundColor);

            if (!"pending".equals(orderStatus)) {
                holder.status.setBackgroundResource(android.R.color.transparent);
                holder.status.setTextColor(backgroundColor);
            }
        }
    }

    @Override
    public int getItemCount() {
        int count = mSoldList != null ? mSoldList.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    // 更新数据方法
    public void updateData(List<Sold> newList) {
        Log.d(TAG, "updateData，新数据大小: " + (newList != null ? newList.size() : 0));

        if (mSoldList == null) {
            mSoldList = new ArrayList<>();
        } else {
            mSoldList.clear();
        }

        if (newList != null) {
            mSoldList.addAll(newList);
        }

        Log.d(TAG, "更新后数据大小: " + mSoldList.size());
        notifyDataSetChanged();
    }
}