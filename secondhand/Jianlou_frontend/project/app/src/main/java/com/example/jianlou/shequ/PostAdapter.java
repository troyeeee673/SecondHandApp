package com.example.jianlou.shequ;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.R;
import com.example.jianlou.my.CircleTransform;
import com.example.jianlou.my.RoundTransform;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * 社区中发布的帖子的类的RecyclerView瀑布流适配器
 */
public class PostAdapter extends RecyclerView.Adapter<PostAdapter.ViewHolder> {
    private List<Post> mPostList;

    static class ViewHolder extends RecyclerView.ViewHolder{
        View postView;
        ImageView photo,head;
        TextView user_name,love,talk,content,origin;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            photo=itemView.findViewById(R.id.shequ_photo);
            head=itemView.findViewById(R.id.shequ_head_photo);
            content=itemView.findViewById(R.id.shequ_content);
            love=itemView.findViewById(R.id.shequ_love);
            user_name=itemView.findViewById(R.id.shequ_user_name);
            talk=itemView.findViewById(R.id.shequ_talk);
            origin=itemView.findViewById(R.id.shequ_origin);
            postView=itemView;
        }
    }

    public PostAdapter(List<Post> postList){
        mPostList=postList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shequ_recycler_good,parent,false);
        final ViewHolder holder=new ViewHolder(view);
        holder.postView.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            // 防止越界
            if (position < 0 || position >= mPostList.size()) {
                return;
            }
            Post post = mPostList.get(position); // 修正变量名拼写错误
            // 1. 获取上下文
            Context context = v.getContext();
            // 2. 创建Intent，跳转至PostDetailActivity
            Intent intent = new Intent(context, PostDetailActivity.class);
            // 3. 传递帖子ID（关键参数，需确保Post类有getPostId()方法）
            intent.putExtra("postId", post.getPostId());
            // 4. 启动详情页
            context.startActivity(intent);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post=mPostList.get(position);

        // ========== 修复1：用户头像（补充空URL判断+兜底） ==========
        String headUrl = post.getPostHeadUrl();
        // 新增：判断URL是否为空（null或空字符串）
        boolean isHeadUrlValid = headUrl != null && !headUrl.trim().isEmpty();
        if (post.getPostHeadID() == -1 && isHeadUrlValid) {
            // 网络头像（URL有效）
            Picasso.get()
                    .load(headUrl)
                    .transform(new CircleTransform())
                    .placeholder(R.mipmap.cat) // 加载中占位图
                    .error(R.mipmap.cat) // 加载失败/URL无效时兜底
                    .into(holder.head);
        } else {
            // 本地头像（包括URL无效的情况，兜底为默认头像）
            int headResId = post.getPostHeadID() != -1 ? post.getPostHeadID() : R.mipmap.cat;
            Picasso.get()
                    .load(headResId)
                    .transform(new CircleTransform())
                    .into(holder.head);
        }

        // ========== 修复2：帖子图片（补充空URL判断+兜底） ==========
        String photoUrl = post.getPostPhotoUrl();
        // 新增：判断URL是否为空（null或空字符串）
        boolean isPhotoUrlValid = photoUrl != null && !photoUrl.trim().isEmpty();
        if (post.getPostPhotoID() == -1 && isPhotoUrlValid) {
            // 网络帖子图片（URL有效）
            Picasso.get()
                    .load(photoUrl)
                    .placeholder(R.mipmap.loading)
                    .error(R.mipmap.shequ0) // 加载失败/URL无效时兜底
                    .transform(new RoundTransform(10,0))
                    .into(holder.photo);
        } else {
            // 本地帖子图片（包括URL无效的情况，兜底为默认图片）
            int photoResId = post.getPostPhotoID() != -1 ? post.getPostPhotoID() : R.mipmap.shequ0;
            Picasso.get()
                    .load(photoResId)
                    .placeholder(R.mipmap.loading)
                    .transform(new RoundTransform(10,0))
                    .into(holder.photo);
        }

        // ========== 修复3：文本字段兜底（避免空指针） ==========
        holder.content.setText(post.getPostContent() != null ? post.getPostContent() : "");
        holder.user_name.setText(post.getPostUser_name() != null ? post.getPostUser_name() : "未知用户");
        holder.talk.setText(post.getPostTalk() != null ? post.getPostTalk() : "#默认话题#");
        holder.love.setText(post.getPostLove() != null ? post.getPostLove() : "0");
        holder.origin.setText(post.getPostOrigin() != null ? post.getPostOrigin() : "来自未知社区");
    }

    @Override
    public int getItemCount() {
        return mPostList.size();
    }
}