package com.example.jianlou.Login;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.jianlou.Activity.MainActivity;
import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Iterator;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
public class Login extends AppCompatActivity implements View.OnClickListener {
    private EditText phone_number, password;
    private ImageView clean_phone, clean_password, show_password;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        init();
    }

    private void init() {
        // 控件绑定
        phone_number = findViewById(R.id.login_phone_number);
        password = findViewById(R.id.login_password);
        clean_phone = findViewById(R.id.login_clean_phone);
        clean_password = findViewById(R.id.login_clean_password);
        show_password = findViewById(R.id.login_show_pwd);
        progressBar = findViewById(R.id.login_progress);
        Button login = findViewById(R.id.login_login);
        TextView register = findViewById(R.id.login_register);

        // 点击事件绑定
        clean_phone.setOnClickListener(this);
        clean_password.setOnClickListener(this);
        show_password.setOnClickListener(this);
        login.setOnClickListener(this);
        register.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_clean_phone:
                phone_number.setText(null);
                break;
            case R.id.login_clean_password:
                password.setText(null);
                break;
            case R.id.login_show_pwd:
                int inputType = password.getInputType();
                if ((inputType & android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) ==
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                    // 密码当前是隐藏状态 → 改为可见
                    password.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    show_password.setImageResource(R.mipmap.pass_visuable);
                } else {
                    // 密码当前是可见状态 → 改为隐藏
                    password.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    show_password.setImageResource(R.mipmap.pass_gone);
                }
                password.setSelection(password.getText().length());
                break;
            case R.id.login_login:
                loginCheck();//处理登录逻辑
                break;
            case R.id.login_register:
                startActivity(new Intent(this, Register.class));
                break;
        }
    }

    private void loginCheck() {
        String phoneNumber = phone_number.getText().toString().trim();
        String pwd = password.getText().toString().trim();
        //判断手机号和密码不为空
        if (phoneNumber.isEmpty() || pwd.isEmpty()) {
            Toast.makeText(this, "请填写账号密码", Toast.LENGTH_SHORT).show();
            return;
        }
        //判断手机号长度
        if (phoneNumber.length() != 11) {
            Toast.makeText(this, "请输入11位手机号", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        RequestBody requestBody = new FormBody.Builder()
                .add("account", phoneNumber)
                .add("password", pwd)
                .build();

        HttpUtil.sendOkHttpRequest(StaticVar.userUrl, requestBody, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(Login.this, "登录失败：网络异常", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> progressBar.setVisibility(View.GONE));

                // 获取手机号（将作为用户账号）
                final String phoneNumber = phone_number.getText().toString().trim();

                if (response.isSuccessful() && response.body() != null) {
                    String responseData = response.body().string().trim();

                    try {
                        // 第一步：无论如何先设置账号
                        StaticVar.user_account = phoneNumber;

                        // 尝试解析JSON
                        if (responseData.startsWith("{") && responseData.endsWith("}")) {
                            JSONObject jsonObject = new JSONObject(responseData);

                            Iterator<String> keys = jsonObject.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                            }

                            // 设置用户信息
                            if (jsonObject.has("cookie")) {
                                StaticVar.cookie = jsonObject.getString("cookie");
                            }
                            if (jsonObject.has("user_name")) {
                                StaticVar.user_name = jsonObject.getString("user_name");
                            }
                            if (jsonObject.has("user_status")) {
                                StaticVar.user_status = jsonObject.getString("user_status");
                            }
                            if (jsonObject.has("user_type")) {
                                StaticVar.user_type = jsonObject.getString("user_type");
                            }

                            // 如果JSON中有account字段，使用它
                            if (jsonObject.has("account")) {
                                String jsonAccount = jsonObject.getString("account");
                                StaticVar.account = jsonAccount;
                                if (jsonAccount != null && !jsonAccount.isEmpty()) {
                                    StaticVar.user_account = jsonAccount;
                                }
                            }
                            StaticVar.headUrl = jsonObject.optString("user_head", "");


                            // 保存到SharedPreferences
                            StaticVar.saveUserInfo(Login.this);

                            // 检查用户状态并跳转
                            if (StaticVar.USER_TYPE_ADMIN.equals(StaticVar.user_type)) {
                                runOnUiThread(() -> {
                                    Toast.makeText(Login.this, "管理员登录成功", Toast.LENGTH_SHORT).show();
                                    jumpToTargetPage();
                                    finish();
                                });
                            } else if (StaticVar.USER_STATUS_APPROVED.equals(StaticVar.user_status)) {
                                runOnUiThread(() -> {
                                    Toast.makeText(Login.this, "登录成功", Toast.LENGTH_SHORT).show();
                                    jumpToTargetPage();
                                    finish();
                                });
                            }
                        } else {
                            handleSimpleResponse(responseData, phoneNumber);
                        }

                    } catch (Exception e) {
                        handleSimpleResponse(responseData, phoneNumber);
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(Login.this, "登录失败：服务器错误", Toast.LENGTH_SHORT).show());
                }
            }

            private void handleSimpleResponse(String responseData, String phoneNumber) {
                // 设置基本用户信息
                StaticVar.user_account = phoneNumber;
                StaticVar.user_name = phoneNumber;
                StaticVar.cookie = "";
                StaticVar.user_type = StaticVar.USER_TYPE_USER; // 默认设为普通用户

                String lowerResponse = responseData.toLowerCase().trim();

                switch (lowerResponse) {
                    case "pending":
                        StaticVar.user_status = StaticVar.USER_STATUS_PENDING;
                        StaticVar.saveUserInfo(Login.this);
                        runOnUiThread(() -> Toast.makeText(Login.this, "你的账号正在审核中，请等待管理员处理", Toast.LENGTH_LONG).show());
                        break;

                    case "rejected":
                        StaticVar.user_status = StaticVar.USER_STATUS_REJECTED;
                        StaticVar.saveUserInfo(Login.this);
                        runOnUiThread(() -> Toast.makeText(Login.this, "你的账号审核未通过，无法登录", Toast.LENGTH_LONG).show());
                        break;

                    case "banned":
                        StaticVar.user_status = StaticVar.USER_STATUS_BANNED;
                        StaticVar.saveUserInfo(Login.this);
                        runOnUiThread(() -> Toast.makeText(Login.this, "你的账号已被禁止，无法登录", Toast.LENGTH_LONG).show());
                        break;

                    case "failed":
                        runOnUiThread(() -> Toast.makeText(Login.this, "账号或密码错误", Toast.LENGTH_SHORT).show());
                        break;
                }
            }
        });
    }

    private void jumpToTargetPage() {
        Intent intent = new Intent(this, MainActivity.class);//跳转到主界面
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}