package com.example.jianlou.index;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.R;
import com.example.jianlou.my.RoundTransform;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * 商品详情页图片适配器
 */
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.ViewHolder> {
    //直接绑定外部列表引用，不初始化空列表
    private List<Photo> photoList;

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView photo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.photo_item_photo);
        }
    }

    //直接绑定外部列表引用，确保数据一致
    public PhotoAdapter(List<Photo> photoList) {
        this.photoList = photoList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载详情页图片item布局（确保布局适配多图显示）
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.setail_photo_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // 越界防护（避免数组越界崩溃，导致图片显示不全）
        if (position >= photoList.size()) {
            return;
        }

        Photo photo = photoList.get(position);
        // Picasso适配多图显示，添加fit()/centerCrop()防止图片变形/截断
        Picasso.get()
                .load(photo.getPhotoPhotoID()) // 对应Photo类的方法名
                .placeholder(R.mipmap.loading) // 加载中占位图
                .error(R.mipmap.ic_launcher)   // 加载失败占位图
                .transform(new RoundTransform(10, 0)) // 圆角保持不变
                .fit() // 自适应ImageView大小
                .centerCrop() // 居中裁剪，避免图片拉伸/显示不全
                .into(holder.photo);
    }

    @Override
    public int getItemCount() {
        // 确保返回真实数量，空列表返回0
        return photoList == null ? 0 : photoList.size();
    }

    // 保留刷新方法（兼容逻辑，实际已用原生刷新）
    public void refreshData(List<Photo> newData) {
        this.photoList = newData;
        notifyDataSetChanged(); // 强制刷新列表
    }
}