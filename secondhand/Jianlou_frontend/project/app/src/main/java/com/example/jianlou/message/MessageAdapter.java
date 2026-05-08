package com.example.jianlou.message;


import android.content.Intent;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jianlou.R;
import com.example.jianlou.my.CircleTransform;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * 商品类的适配器，用于RecyclerView空间的瀑布平显示
 */
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<Message> mmessageList;
    private boolean longClicked = false;
    private int position;
    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        ImageView head;
        TextView message, user_name;
        TextView tvUnreadCount; // 新增：未读标记控件
        View messageView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            head = itemView.findViewById(R.id.message_friend_head);
            user_name = itemView.findViewById(R.id.message_friend_user_name);
            message = itemView.findViewById(R.id.message_friend_message);
            tvUnreadCount = itemView.findViewById(R.id.tv_unread_count); // 绑定未读控件
            messageView = itemView;
            messageView.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            menu.add(ContextMenu.NONE, 0, ContextMenu.NONE, "置顶聊天");
            menu.add(ContextMenu.NONE, 1, ContextMenu.NONE, "删除聊天");
        }
    }

    public MessageAdapter(List<Message> messageList) {
        mmessageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_friend_recycler, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.messageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (longClicked) {
                        longClicked = false;
                        holder.messageView.setBackgroundResource(R.color.white);
                    } else {
                        int position =holder.getAdapterPosition();
                        Message message =mmessageList.get(position);
                        holder.messageView.setBackgroundResource(R.color.color_eeeeee);
                        Intent intent=new Intent(v.getContext(),chat.class);
                        intent.putExtra("username",message.geMessagetUsername());
                        intent.putExtra("friend_name",message.getMessageUser_name());
                        // 传递goodsId参数
                        intent.putExtra("goodsId", message.getGoodsId());

                        // 进入聊天界面时，保存已读时间戳，清零未读数量
                        String friendUsername = message.geMessagetUsername();
                        String goodsId = message.getGoodsId();
                        String lastMsgTime = message.getLastMsgTime();

                        ((message_friend)v.getContext()).saveReadTime(friendUsername, goodsId, lastMsgTime);
                        message.setUnreadCount(0);
                        notifyItemChanged(position); // 刷新当前项

                        v.getContext().startActivity(intent);
                    }
                }
                return false;
            }
        });
        holder.messageView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                int position = holder.getAdapterPosition();
                setPosition(position);
                holder.messageView.setBackgroundResource(R.color.color_eeeeee);
                longClicked = true;
                return false;
            }
        });

        return holder;
    }

    /**
     * 修改空间的显示的
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = mmessageList.get(position);
        String headUrl = message.getMessageHeadUrl();
        int unreadCount = message.getUnreadCount(); // 获取未读数量

        // 加载头像：有效URL加载网络图片，无效显示默认头像
        boolean isHeadValid = (headUrl != null && !headUrl.trim().isEmpty());
        if (isHeadValid) {
            Picasso.get()
                    .load(headUrl)
                    .transform(new CircleTransform())
                    .placeholder(R.mipmap.cat)
                    .error(R.mipmap.cat)
                    .into(holder.head);
        } else {
            Picasso.get()
                    .load(R.mipmap.cat)
                    .transform(new CircleTransform())
                    .into(holder.head);
        }

        holder.message.setText(message.getMessageMessage());
        holder.user_name.setText(message.getMessageUser_name());

        // 新增：未读标记显示逻辑
        if (unreadCount > 0) {
            holder.tvUnreadCount.setVisibility(View.VISIBLE);
            // 未读数超过99显示“99+”，否则显示具体数字
            holder.tvUnreadCount.setText(unreadCount > 99 ? "99+" : String.valueOf(unreadCount));
        } else {
            holder.tvUnreadCount.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return mmessageList.size();
    }

    public void removeData(int position) {
        mmessageList.remove(position);
        notifyItemRemoved(position);
        if (position != mmessageList.size()) {
            notifyItemRangeChanged(position, mmessageList.size() - position);
        }
    }


    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }


}