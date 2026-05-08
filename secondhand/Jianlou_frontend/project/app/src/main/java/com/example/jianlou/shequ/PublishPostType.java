package com.example.jianlou.shequ;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jianlou.R;

// 帖子类型选择页面（参照商品分类选择页面逻辑）
public class PublishPostType extends AppCompatActivity {
    private String[] selectedTypes; // 选中的帖子类型

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_post_type);

        // 示例：绑定类型选择按钮（如“动态”“求助”“分享”）
        TextView tvTypeDynamic = findViewById(R.id.tv_type_dynamic);
        tvTypeDynamic.setOnClickListener(v -> {
            selectedTypes = new String[]{"动态更新"};
            returnResult();
        });
        TextView help = findViewById(R.id.tv_type_help);
        help.setOnClickListener(v -> {
            selectedTypes = new String[]{"打听求助"};
            returnResult();
        });
        TextView share = findViewById(R.id.tv_type_share);
        share.setOnClickListener(v -> {
            selectedTypes = new String[]{"趣味分享"};
            returnResult();
        });
        TextView talk = findViewById(R.id.tv_type_talk);
        talk.setOnClickListener(v -> {
            selectedTypes = new String[]{"我要吐槽"};
            returnResult();
        });
    }

    // 选择完成后返回结果给帖子发布页面
    private void returnResult() {
        Intent intent = new Intent();
        intent.putExtra("postType", selectedTypes);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}