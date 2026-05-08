package com.example.jianlou.message;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
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

// 消息适配器
class MSgAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Msg> mMsgList;
    private final int DEFAULT_AVATAR = R.mipmap.cat; // 默认头像

    public MSgAdapter(List<Msg> msgList) {
        mMsgList = msgList;
    }

    /**
     * 根据布局类型返回对应ViewHolder
     */
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == Msg.TYPE_TIME) {
            // 时间戳布局
            View view = inflater.inflate(R.layout.item_msg_time, parent, false);
            return new TimeViewHolder(view);
        } else if (viewType == Msg.TYPE_RECEIVED) {
            // 接收消息布局
            View view = inflater.inflate(R.layout.item_msg_received, parent, false);
            return new MsgViewHolder(view);
        } else {
            // 发送消息布局
            View view = inflater.inflate(R.layout.item_msg_sent, parent, false);
            return new MsgViewHolder(view);
        }
    }

    /**
     * 绑定布局数据
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Msg msg = mMsgList.get(position);
        if (holder instanceof TimeViewHolder) {
            // 绑定时间戳数据
            ((TimeViewHolder) holder).tvTime.setText(msg.getFormatTime());
        } else if (holder instanceof MsgViewHolder) {
            // 绑定消息数据
            MsgViewHolder msgHolder = (MsgViewHolder) holder;
            msgHolder.msgContent.setText(msg.getContent());

            // 加载头像
            // 1. 获取Context（从ViewHolder的itemView中获取）
            Context context = msgHolder.itemView.getContext();

            // 2. 处理头像URL/资源ID，统一转为Uri
            String avatarUrl = msg.getHeadUrl();
            Uri targetUri;
            if (TextUtils.isEmpty(avatarUrl)) {
                // 资源ID转Uri（格式：android.resource://[包名]/[资源ID]）
                String defaultAvatarUriStr = String.format("android.resource://%s/%d",
                        context.getPackageName(), DEFAULT_AVATAR);
                targetUri = Uri.parse(defaultAvatarUriStr);
            } else {
                // 网络URL转Uri
                targetUri = Uri.parse(avatarUrl);
            }

            // 3. 加载Uri类型的资源
            Picasso.get()
                    .load(targetUri)
                    .transform(new CircleTransform())
                    .placeholder(R.mipmap.cat)
                    .error(R.mipmap.cat)
                    .fit()
                    .centerCrop()
                    .into(msgHolder.avatar);
        }
    }

    @Override
    public int getItemCount() {
        return mMsgList.size();
    }

    /**
     * 返回当前位置的布局类型
     */
    @Override
    public int getItemViewType(int position) {
        return mMsgList.get(position).getType();
    }

    /**
     * 消息ViewHolder（接收/发送消息共用）
     */
    class MsgViewHolder extends RecyclerView.ViewHolder {
        TextView msgContent;
        ImageView avatar;

        public MsgViewHolder(View itemView) {
            super(itemView);
            msgContent = itemView.findViewById(R.id.msg_content);
            avatar = itemView.findViewById(R.id.avatar);
        }
    }

    /**
     * 时间戳ViewHolder
     */
    class TimeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;

        public TimeViewHolder(View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tv_msg_time);
        }
    }
}