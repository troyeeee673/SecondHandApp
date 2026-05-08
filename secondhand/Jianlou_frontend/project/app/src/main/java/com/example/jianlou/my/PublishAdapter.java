package com.example.jianlou.my;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.R;
import com.example.jianlou.index.good_detail;
import com.example.jianlou.staticVar.StaticVar;
import com.squareup.picasso.Picasso;

import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 修改后的PublishAdapter，支持删除功能
 */
public class PublishAdapter extends RecyclerView.Adapter<PublishAdapter.ViewHolder> {
    // 直接使用外部传入的列表（和myPublish的goodList共享引用）
    private final List<Publish> mGoodList;
    private OnItemDeleteListener mDeleteListener;

    // 定义删除监听器接口
    public interface OnItemDeleteListener {
        void onItemDelete(String goodID, int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;
        TextView content, money, status; // 添加status TextView
        Button deleteButton;
        View goodView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.my_publish_photo);
            content = itemView.findViewById(R.id.my_publish_content);
            money = itemView.findViewById(R.id.my_publish_money);
            status = itemView.findViewById(R.id.my_publish_status); // 初始化status TextView
            deleteButton = itemView.findViewById(R.id.delete_button);
            goodView = itemView;
        }
    }

    // 构造方法
    public PublishAdapter(List<Publish> goodList) {
        this.mGoodList = goodList;
    }

    // 设置删除监听器
    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.mDeleteListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.publish_recycler_good, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Publish good = mGoodList.get(position);

        // 绑定商品数据
        Picasso.get()
                .load(good.getPhotoID())
                .placeholder(R.mipmap.loading)
                .error(R.mipmap.ic_launcher)
                .transform(new RoundTransform(10, 10))
                .into(holder.photo);
        holder.content.setText(good.getContent());
        holder.money.setText(good.getMoney());

        // 设置商品状态显示
        setStatusDisplay(holder, good);

        // 设置商品点击事件
        holder.goodView.setOnClickListener(v -> {
            if (position >= 0 && position < mGoodList.size()) {
                Publish clickedGood = mGoodList.get(position);
                Intent intent = new Intent(v.getContext(), good_detail.class);
                intent.putExtra("goodsID", clickedGood.getGoodID());
                v.getContext().startActivity(intent);
            }
        });
    }

    /**
     * 设置商品状态显示和删除按钮状态
     */
    private void setStatusDisplay(ViewHolder holder, Publish good) {
        String statusText = "";
        int textColor = Color.BLACK;
        int bgColor = 0xFFCCCCCC; // 默认灰色背景

        if (good.isSold()) {
            statusText = "已售出";
            textColor = Color.WHITE;
            bgColor = 0xFF4CAF50; // 绿色背景表示已售出

            // 已售出的商品：按钮灰色且不可点击
            holder.deleteButton.setEnabled(false);
            holder.deleteButton.setBackgroundColor(0xFFCCCCCC); // 灰色背景
            holder.deleteButton.setText("已售出");
            holder.deleteButton.setOnClickListener(null); // 移除点击事件
        } else {
            statusText = "在售中";
            textColor = Color.WHITE;
            bgColor = 0xFFFF9800; // 橙色背景表示在售

            // 未售出的商品：正常状态
            holder.deleteButton.setEnabled(true);
            holder.deleteButton.setBackgroundColor(0xFFFF3B30); // 红色背景
            holder.deleteButton.setText("删除");

            // 设置删除按钮点击事件
            holder.deleteButton.setOnClickListener(v -> {
                int position = holder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && mDeleteListener != null) {
                    Publish clickedGood = mGoodList.get(position);

                    // 再次检查是否已售出（防止并发问题）
                    if (clickedGood.isSold()) {
                        Toast.makeText(v.getContext(), "已售出的商品不能删除", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // 显示确认对话框
                    new AlertDialog.Builder(v.getContext())
                            .setTitle("确认删除")
                            .setMessage("确定要删除商品《" + clickedGood.getContent() + "》吗？")
                            .setPositiveButton("确定", (dialog, which) -> {
                                // 调用删除监听器
                                mDeleteListener.onItemDelete(clickedGood.getGoodID(), position);
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
            });
        }

        // 设置状态文本和样式
        holder.status.setText(statusText);
        holder.status.setTextColor(textColor);
        holder.status.setBackgroundColor(bgColor);
    }

    @Override
    public int getItemCount() {
        return mGoodList == null ? 0 : mGoodList.size();
    }

    // 提供删除方法供Activity调用
    public void removeItem(int position) {
        if (position >= 0 && position < mGoodList.size()) {
            mGoodList.remove(position);
            notifyItemRemoved(position);
            // 通知后面的item位置变化
            notifyItemRangeChanged(position, mGoodList.size() - position);
        }
    }
}