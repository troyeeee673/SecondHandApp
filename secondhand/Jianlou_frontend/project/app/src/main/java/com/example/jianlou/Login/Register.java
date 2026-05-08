package com.example.jianlou.Login;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Register extends AppCompatActivity implements View.OnClickListener {
    private EditText phone_number, password, password_again;//声明手机号、密码、确认密码组件
    private ImageView clean_phone, clean_password, show_password, clean_password_again, show_password_again;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        init();
    }

    private void init() {
        // 初始化控件
        phone_number = findViewById(R.id.register_phone_number);
        password = findViewById(R.id.register_password);
        password_again = findViewById(R.id.register_password_again);
        clean_phone = findViewById(R.id.register_clean_phone);
        clean_password = findViewById(R.id.register_clean_password);
        clean_password_again = findViewById(R.id.register_clean_password_again);
        show_password = findViewById(R.id.register_show_pwd);
        show_password_again = findViewById(R.id.register_show_pwd_again);
        progressBar = findViewById(R.id.register_progress);
        Button register = findViewById(R.id.register_register);
        TextView login = findViewById(R.id.register_login);

        // 绑定点击事件
        clean_phone.setOnClickListener(this);
        clean_password.setOnClickListener(this);
        clean_password_again.setOnClickListener(this);
        show_password.setOnClickListener(this);
        show_password_again.setOnClickListener(this);
        register.setOnClickListener(this);
        login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.register_clean_phone:
                phone_number.setText(null);
                break;
            case R.id.register_clean_password:
                password.setText(null);
                break;
            case R.id.register_clean_password_again:
                password_again.setText(null);
                break;
            case R.id.register_show_pwd:
                // 切换密码可见性
                if (password.getInputType() == 129) {
                    password.setInputType(128);
                    show_password.setImageResource(R.mipmap.pass_visuable);
                } else {
                    password.setInputType(129);
                    show_password.setImageResource(R.mipmap.pass_gone);
                }
                break;
            case R.id.register_show_pwd_again:
                // 切换确认密码可见性
                if (password_again.getInputType() == 129) {
                    password_again.setInputType(128);
                    show_password_again.setImageResource(R.mipmap.pass_visuable);
                } else {
                    password_again.setInputType(129);
                    show_password_again.setImageResource(R.mipmap.pass_gone);
                }
                break;
            case R.id.register_register:
                RegisterCheck();
                break;
            case R.id.register_login:
                finish();
                break;
        }
    }


    // 注册信息校验 & 提交注册请求
    private void RegisterCheck() {
        String phoneNumber = phone_number.getText().toString().trim();
        String pwd = password.getText().toString().trim();
        String pwd_again = password_again.getText().toString().trim();

        // 前端校验
        if (phoneNumber.isEmpty() || pwd.isEmpty() || pwd_again.isEmpty()) {
            Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phoneNumber.length() != 11) {
            Toast.makeText(this, "请输入11位手机号", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!pwd.equals(pwd_again)) {
            Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        // 发起网络请求前：显示显示加载中
        progressBar.setVisibility(View.VISIBLE);

        // 构建请求体：提交账号、密码、默认pending状态
        RequestBody requestBody = new FormBody.Builder()
                .add("account", phoneNumber)  //与后端参数名一致
                .add("password", pwd)
                .add("user_status", StaticVar.USER_STATUS_PENDING) //用户默认状态
                .add("user_type", StaticVar.USER_TYPE_USER) //用户默认类型
                .build();

        // 发送注册POST请求  StaticVar.registerUrl注册接口的服务器地址 Callback网络请求的回调接口
        HttpUtil.sendOkHttpRequest(StaticVar.registerUrl, requestBody, new okhttp3.Callback() {
            //请求失败回调
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);//隐藏进度条
                    Toast.makeText(Register.this, "注册失败：网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            //请求成功回调
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string().trim(); //获取后端返回的字符串
                    try {
                        // 解析后端JSON响应
                        JSONObject jsonObject = new JSONObject(responseData);
                        String code = jsonObject.getString("code");
                        String msg = jsonObject.getString("msg");

                        runOnUiThread(() -> {
                            Toast.makeText(Register.this, msg, Toast.LENGTH_LONG).show();//将后端返回的信息提示给用户
                            // 注册成功（提交审核）则关闭注册页面
                            if ("success".equals(code)) {
                                finish();
                            }
                        });

                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            Toast.makeText(Register.this, "注册失败：数据解析错误", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(Register.this, "注册失败：服务器错误", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }
}