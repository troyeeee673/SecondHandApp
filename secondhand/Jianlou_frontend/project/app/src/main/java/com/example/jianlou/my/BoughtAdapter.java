package com.example.jianlou.my;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.R;
import com.example.jianlou.index.good_detail;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class BoughtAdapter extends RecyclerView.Adapter<BoughtAdapter.ViewHolder> {
    private List<Bought> mBoughtList;
    private myBought activity; // 新增：用于按钮点击事件
    private static final String TAG = "BoughtAdapter";

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView content, money, seller, time, orderId, status; // 新增 status
        Button btnConfirm, btnReturn;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.bought_photo);
            content = itemView.findViewById(R.id.bought_content);
            money = itemView.findViewById(R.id.bought_money);
            seller = itemView.findViewById(R.id.bought_seller);
            time = itemView.findViewById(R.id.bought_time);
            orderId = itemView.findViewById(R.id.bought_order_id);
            status = itemView.findViewById(R.id.bought_status); // 新增
            btnConfirm = itemView.findViewById(R.id.btn_confirm);
            btnReturn = itemView.findViewById(R.id.btn_return);
        }
    }

    // 修改构造函数，接受activity参数
    public BoughtAdapter(List<Bought> boughtList, myBought activity) {
        // 关键修改：直接使用传入的列表，而不是创建新列表
        if (boughtList == null) {
            this.mBoughtList = new ArrayList<>();
        } else {
            this.mBoughtList = boughtList;
        }
        this.activity = activity;
        Log.d(TAG, "适配器创建，初始数据大小: " + mBoughtList.size());
    }

    // 原有的构造函数（为了向后兼容）
    public BoughtAdapter(List<Bought> boughtList) {
        this(boughtList, null);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "创建ViewHolder");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bought_recycler_good, parent, false);
        final ViewHolder holder = new ViewHolder(view);

        // 设置整个item的点击事件（跳转到商品详情）
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION &&
                        mBoughtList != null &&
                        position < mBoughtList.size()) {

                    Bought clickedBought = mBoughtList.get(position);
                    Log.d(TAG, "点击订单: " + clickedBought.getContent());

                    if (clickedBought.getGoodsId() != null && !clickedBought.getGoodsId().isEmpty()) {
                        Intent intent = new Intent(v.getContext(), good_detail.class);
                        intent.putExtra("goodsID", clickedBought.getGoodsId());
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

        if (mBoughtList == null || adapterPosition < 0 || adapterPosition >= mBoughtList.size()) {
            Log.e(TAG, "无效的position: " + adapterPosition + ", 列表大小: " + (mBoughtList != null ? mBoughtList.size() : 0));
            return;
        }

        Bought bought = mBoughtList.get(adapterPosition);

        // 使用final变量用于Picasso回调
        final int finalAdapterPosition = adapterPosition;

        Log.d(TAG, "绑定位置 " + finalAdapterPosition + ": " + bought.getContent() + ", 价格: " + bought.getMoney() + ", 状态: " + bought.getOrderStatus());

        // 绑定商品数据
        if (holder.photo != null && bought.getPhotoID() != null) {
            Log.d(TAG, "加载图片: " + bought.getPhotoID().toString());

            Picasso.get()
                    .load(bought.getPhotoID())
                    .placeholder(R.mipmap.loading)
                    .error(R.mipmap.ic_launcher)
                    .into(holder.photo, new com.squareup.picasso.Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "图片加载成功，位置: " + finalAdapterPosition);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "图片加载失败，位置: " + finalAdapterPosition + ", " + e.getMessage());
                        }
                    });
        } else {
            Log.w(TAG, "图片或ImageView为空，使用默认图片");
            if (holder.photo != null) {
                holder.photo.setImageResource(R.mipmap.ic_launcher);
            }
        }

        // 设置文本内容
        if (holder.content != null) {
            holder.content.setText(bought.getContent());
        }

        if (holder.money != null) {
            holder.money.setText(bought.getMoney());
        }

        if (holder.seller != null) {
            holder.seller.setText(bought.getFullSellerInfo());
        }

        if (holder.time != null) {
            holder.time.setText(bought.getFullCreateTime());
        }

        // 设置订单号
        if (holder.orderId != null) {
            holder.orderId.setText("订单号：" + bought.getOrderId());
        }

        // 设置订单状态显示
        if (holder.status != null) {
            String orderStatus = bought.getOrderStatus();
            String statusText;
            int backgroundColor;

            // 根据状态设置不同的文本和颜色
            switch (orderStatus) {
                case "pending":
                    statusText = "待收货";
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

            // 如果状态不是pending，隐藏状态标签的背景色
            if (!"pending".equals(orderStatus)) {
                holder.status.setBackgroundResource(android.R.color.transparent);
                holder.status.setTextColor(backgroundColor); // 直接使用颜色作为文字颜色
            }
        }

        // 设置按钮
        if (holder.btnConfirm != null) {
            // 根据订单状态显示/隐藏确认收货按钮
            String status = bought.getOrderStatus();
            if ("pending".equals(status)) {
                Log.d(TAG, "位置 " + finalAdapterPosition + ": 显示确认收货按钮");
                holder.btnConfirm.setVisibility(View.VISIBLE);
                holder.btnConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "确认收货按钮点击，位置: " + finalAdapterPosition);
                        if (activity != null) {
                            activity.onConfirmReceiptClick(bought.getOrderId(), finalAdapterPosition);
                        }
                    }
                });
            } else {
                Log.d(TAG, "位置 " + finalAdapterPosition + ": 隐藏确认收货按钮，状态: " + status);
                holder.btnConfirm.setVisibility(View.GONE);
                // 移除点击监听器避免内存泄漏
                holder.btnConfirm.setOnClickListener(null);
            }
        }

        // 设置退货按钮
        if (holder.btnReturn != null) {
            // 根据订单状态显示/隐藏退货按钮
            String status = bought.getOrderStatus();
            if ("pending".equals(status)) {
                Log.d(TAG, "位置 " + finalAdapterPosition + ": 显示退货按钮");
                holder.btnReturn.setVisibility(View.VISIBLE);

                // 简化版本：直接退货，不需要选择原因
                holder.btnReturn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d(TAG, "退货按钮点击，订单ID: " + bought.getOrderId());

                        // 1. 直接调用activity的退货方法，不需要原因参数
                        if (activity != null) {
                            // 这里调用一个简化版的退货方法，不需要原因参数
                            activity.applyReturnSimple(bought.getOrderId(), finalAdapterPosition);
                        } else {
                            Log.e(TAG, "activity为null，无法处理退货");
                        }
                    }
                });

            } else {
                Log.d(TAG, "位置 " + finalAdapterPosition + ": 隐藏退货按钮，状态: " + status);
                holder.btnReturn.setVisibility(View.GONE);
                holder.btnReturn.setOnClickListener(null);
            }
        }

//
//        if (holder.btnReturn != null) {
//            // 根据订单状态显示/隐藏退货按钮
//            String status = bought.getOrderStatus();
//            if ("pending".equals(status)) {
//                Log.d(TAG, "位置 " + finalAdapterPosition + ": 显示退货按钮");
//                holder.btnReturn.setVisibility(View.VISIBLE);
//                // 设置退货按钮 - 简化版本
//                holder.btnReturn.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        // 直接在适配器中显示对话框
//                        String[] reasons = new String[]{"商品质量问题", "商品与描述不符", "发错商品", "其他原因"};
//
//                        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
//                        builder.setTitle("申请退货");
//                        builder.setMessage("请选择退货原因");
//                        builder.setItems(reasons, new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                String reason = reasons[which];
//                                Toast.makeText(v.getContext(),
//                                        "选择了原因: " + reason,
//                                        Toast.LENGTH_SHORT).show();
//
//                                // 调用activity的方法处理退货
//                                if (activity != null) {
//                                    activity.applyReturn(bought.getOrderId(), finalAdapterPosition, reason);
//                                }
//                            }
//                        });
//                        builder.setNegativeButton("取消", null);
//
//                        AlertDialog dialog = builder.create();
//                        dialog.show();
//                    }
//                });
//            } else {
//                Log.d(TAG, "位置 " + finalAdapterPosition + ": 隐藏退货按钮，状态: " + status);
//                holder.btnReturn.setVisibility(View.GONE);
//                // 移除点击监听器避免内存泄漏
//                holder.btnReturn.setOnClickListener(null);
//            }
//        }

        // 调试信息：打印按钮的实际可见性
        if (holder.btnConfirm != null && holder.btnReturn != null) {
            Log.d(TAG, "位置 " + finalAdapterPosition + " 按钮状态 - " +
                    "确认收货: " + (holder.btnConfirm.getVisibility() == View.VISIBLE ? "显示" : "隐藏") +
                    ", 退货: " + (holder.btnReturn.getVisibility() == View.VISIBLE ? "显示" : "隐藏"));
        }


//        // 设置退货按钮
//        if (holder.btnReturn != null) {
//            // 根据订单状态显示/隐藏退货按钮
//            String status = bought.getOrderStatus();
//            if ("pending".equals(status)) {
//                Log.d(TAG, "位置 " + finalAdapterPosition + ": 显示退货按钮");
//                holder.btnReturn.setVisibility(View.VISIBLE);
//                holder.btnReturn.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        Log.d(TAG, "退货按钮被点击，位置: " + finalAdapterPosition +
//                                ", 订单ID: " + bought.getOrderId() +
//                                ", 状态: " + bought.getOrderStatus());
//
//                        // 测试按钮是否正常工作
//                        Toast.makeText(v.getContext(),
//                                "点击测试: 订单" + bought.getOrderId(),
//                                Toast.LENGTH_SHORT).show();
//
//                        if (activity != null) {
//                            activity.onReturnClick(bought.getOrderId(), finalAdapterPosition);
//                        } else {
//                            Log.e(TAG, "activity为null，无法处理点击事件");
//                        }
//                    }
//                });
//
//            } else {
//                Log.d(TAG, "位置 " + finalAdapterPosition + ": 隐藏退货按钮，状态: " + status);
//                holder.btnReturn.setVisibility(View.GONE);
//                holder.btnReturn.setOnClickListener(null);
//            }
//        }

        // 调试按钮点击
        if (holder.btnReturn != null) {
            holder.btnReturn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Log.d(TAG, "退货按钮长按测试，位置: " + finalAdapterPosition);
                    Toast.makeText(v.getContext(),
                            "长按测试: 按钮有效",
                            Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        int count = mBoughtList != null ? mBoughtList.size() : 0;
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    // 更新数据方法
    public void updateData(List<Bought> newList) {
        Log.d(TAG, "updateData，新数据大小: " + (newList != null ? newList.size() : 0));

        if (mBoughtList == null) {
            mBoughtList = new ArrayList<>();
        } else {
            mBoughtList.clear();
        }

        if (newList != null) {
            // 关键：确保我们添加的是新列表的数据
            mBoughtList.addAll(newList);
        }

        Log.d(TAG, "更新后数据大小: " + mBoughtList.size());
        notifyDataSetChanged();
    }
    // 添加方法用于调试
    public Object getActivityReference() {
        return activity;
    }
}