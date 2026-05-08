package com.example.jianlou.shequ;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.jianlou.R;

public class PublishPostCommunity extends AppCompatActivity implements View.OnClickListener {
    private String[] selectedCommunity; // 选中的社区

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_publish_post_community);

        // 绑定社区选项控件
        TextView tvSchool = findViewById(R.id.tv_community_school);
        TextView tvHobby = findViewById(R.id.tv_community_hobby);
        TextView tvTrade = findViewById(R.id.tv_community_trade);
        TextView tvStudy = findViewById(R.id.tv_community_study);

        // 添加点击监听
        tvSchool.setOnClickListener(this);
        tvHobby.setOnClickListener(this);
        tvTrade.setOnClickListener(this);
        tvStudy.setOnClickListener(this);

        // 接收已选中的社区（若有）
        String[] preSelect = getIntent().getStringArrayExtra("postCommunity");
        if (preSelect != null) {
            selectedCommunity = preSelect;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_community_school:
                selectedCommunity = new String[]{"校园生活"};
                returnResult();
                break;
            case R.id.tv_community_hobby:
                selectedCommunity = new String[]{"兴趣爱好"};
                returnResult();
                break;
            case R.id.tv_community_trade:
                selectedCommunity = new String[]{"闲置交易"};
                returnResult();
                break;
            case R.id.tv_community_study:
                selectedCommunity = new String[]{"学习交流"};
                returnResult();
                break;
        }
    }

    // 返回选中的社区给帖子发布页面
    private void returnResult() {
        Intent intent = new Intent();
        intent.putExtra("postCommunity", selectedCommunity);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }
}