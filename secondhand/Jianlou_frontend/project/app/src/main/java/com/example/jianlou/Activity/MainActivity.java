package com.example.jianlou.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.jianlou.R;
import com.example.jianlou.Login.Login;
import com.example.jianlou.message.XiaoXiFragment;
import com.example.jianlou.publish.publishGood.PublishGoodActivity;
import com.example.jianlou.shequ.SheQuFragment;
import com.example.jianlou.index.ShouYeFragment;
import com.example.jianlou.my.WoDeFragment;
import com.example.jianlou.staticVar.StaticVar;

public class MainActivity extends AppCompatActivity {

    private Fragment shouYeFragment, sheQuFragment, xiaoXiFragment, woDeFragment;
    private RadioGroup radioGroup;
    private RadioButton rbShouYe, rbSheQu, rbXiaoXi, rbWoDe;
    private ImageView ivAdd;
    public static final int VIEW_SHOUYE_INDEX = 0;
    public static final int VIEW_SHEQU_INDEX  = 1;
    public static final int VIEW_XIAOXI_INDEX = 3;
    public static final int VIEW_WODE_INDEX   = 4;
    private int currentFragmentIndex = VIEW_SHOUYE_INDEX;
    public static int VIEW_LAST_INDEX = VIEW_SHOUYE_INDEX;
    private int temp_position_index = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StaticVar.loadUserInfo(this);//读取保存到本地的用户变量
        initView(); //对于组件的初始化

        // 处理跳转指令
        handleJumpIntent();

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // 点击消息按钮时：先判断登录状态
            if (checkedId == R.id.id_nav_btxiaoxi) {
                // 未登录：直接跳转到登录页面
                if (!StaticVar.isLogin(MainActivity.this)) {
                    startActivity(new Intent(MainActivity.this, Login.class));
                    return;
                }
                // 已登录：正常切换
                switchView(findViewById(checkedId));
            }
            // 点击我的按钮时：先判断登录状态
            else if (checkedId == R.id.id_nav_btwode) {
                if (!StaticVar.isLogin(MainActivity.this)) {
                    startActivity(new Intent(MainActivity.this, Login.class));
                    return;
                }
                switchView(findViewById(checkedId));
            }
            // 点击社区按钮时，不判断登录状态
            else if (checkedId == R.id.id_nav_btshequ) {
//                if (!StaticVar.isLogin(MainActivity.this)) {
//                    startActivity(new Intent(MainActivity.this, Login.class));
//                    return;
//                }
                switchView(findViewById(checkedId));
            }
            // 首页不需要登录检查
            else if (checkedId == R.id.id_nav_btshouye) {
                switchView(findViewById(checkedId));
            }
        });

    }

    private void initView() {
        shouYeFragment = new ShouYeFragment();
        sheQuFragment = new SheQuFragment();
        xiaoXiFragment = new XiaoXiFragment();
        woDeFragment = new WoDeFragment();

        radioGroup = findViewById(R.id.id_navcontent);
        rbShouYe  = findViewById(R.id.id_nav_btshouye);
        rbSheQu   = findViewById(R.id.id_nav_btshequ);
        rbXiaoXi  = findViewById(R.id.id_nav_btxiaoxi);
        rbWoDe    = findViewById(R.id.id_nav_btwode);
        ivAdd     = findViewById(R.id.id_nav_btadd);

        transview(shouYeFragment, VIEW_SHOUYE_INDEX);

        // 发帖子按钮
        ivAdd.setOnClickListener(v -> {
            StaticVar.loadUserInfo(MainActivity.this);
            if (!StaticVar.isLogin(MainActivity.this)) {//如果还没登陆就跳到登陆页面
                startActivity(new Intent(MainActivity.this, Login.class));
                return;
            }
            if (!StaticVar.isApprovedUser(MainActivity.this)) {
                Toast.makeText(MainActivity.this, "你的账号尚未通过审核，无法发布帖子", Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(MainActivity.this, PublishGoodActivity.class));
        });
    }

    /**
     * 处理跳转到消息页的指令
     */
    private void handleJumpIntent() {
        Intent intent = getIntent();
        boolean needJump = intent.getBooleanExtra("jump_to_message", false);

        if (needJump) {
            // 立即清空跳转标记，避免重复处理
            getIntent().removeExtra("jump_to_message");

            // 先检查登录状态
            if (!StaticVar.isLogin(this)) {
                // 未登录：直接跳登录，终止后续所有逻辑
                startActivity(new Intent(this, Login.class));
                // 确保底部导航显示首页
                rbShouYe.setChecked(true);
                return; // 终止后续逻辑
            }

            // 已登录：显示普通消息页
            transview(xiaoXiFragment, VIEW_XIAOXI_INDEX);
            currentFragmentIndex = VIEW_XIAOXI_INDEX;
            VIEW_LAST_INDEX = VIEW_XIAOXI_INDEX;
            temp_position_index = VIEW_XIAOXI_INDEX;
            // 选中消息按钮
            rbXiaoXi.setChecked(true);
        }
    }

    /**
     * 统一入口：底部导航点击（我后面把管理员删掉了可以不要这个方法）
     */
    public void switchView(View v) {
        StaticVar.loadUserInfo(this);
        switch (v.getId()) {
            case R.id.id_nav_btshouye:
                transview(shouYeFragment, VIEW_SHOUYE_INDEX);
                currentFragmentIndex = VIEW_SHOUYE_INDEX;
                break;
            case R.id.id_nav_btshequ:
                transview(sheQuFragment, VIEW_SHEQU_INDEX);
                currentFragmentIndex = VIEW_SHEQU_INDEX;
                break;
            case R.id.id_nav_btxiaoxi:
                transview(xiaoXiFragment, VIEW_XIAOXI_INDEX);
                currentFragmentIndex = VIEW_XIAOXI_INDEX;
                break;
            case R.id.id_nav_btwode:
                transview(woDeFragment, VIEW_WODE_INDEX);
                currentFragmentIndex = VIEW_WODE_INDEX;
                break;
        }
    }

    //切换页面
    private void transview(Fragment fragment, int index) {
        if (temp_position_index == index) return;
        temp_position_index = index;
        VIEW_LAST_INDEX = index;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        hideAllFragments(transaction);
        if (!fragment.isAdded()) {
            transaction.add(R.id.id_fragment_content, fragment, fragment.getClass().getSimpleName());
        }
        transaction.show(fragment).commit();
    }

    /**
     * 强制切换到首页（用于Fragment中发现未登录时调用）
     */
    public void forceSwitchToHome() {
        runOnUiThread(() -> {
            // 切换到首页
            transview(shouYeFragment, VIEW_SHOUYE_INDEX);
            currentFragmentIndex = VIEW_SHOUYE_INDEX;
            VIEW_LAST_INDEX = VIEW_SHOUYE_INDEX;
            temp_position_index = VIEW_SHOUYE_INDEX;
            // 选中首页按钮
            rbShouYe.setChecked(true);

            startActivity(new Intent(this, Login.class));
        });
    }


    private void hideAllFragments(FragmentTransaction transaction) {
        if (shouYeFragment != null) transaction.hide(shouYeFragment);
        if (sheQuFragment != null) transaction.hide(sheQuFragment);
        if (xiaoXiFragment != null) transaction.hide(xiaoXiFragment);
        if (woDeFragment != null) transaction.hide(woDeFragment);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        temp_position_index = -1;
        StaticVar.loadUserInfo(this);

        // 统一登录校验逻辑
        switch (currentFragmentIndex) {
            case VIEW_SHOUYE_INDEX:
                transview(shouYeFragment, VIEW_SHOUYE_INDEX);
                break;
            case VIEW_SHEQU_INDEX:
                transview(sheQuFragment, VIEW_SHEQU_INDEX);
                break;
            case VIEW_XIAOXI_INDEX:
                // 关键：必须已登录才能显示消息页
                if (!StaticVar.isLogin(this)) {
                    // 未登录：回到首页
                    transview(shouYeFragment, VIEW_SHOUYE_INDEX);
                    currentFragmentIndex = VIEW_SHOUYE_INDEX;
                    rbShouYe.setChecked(true);
                    break;
                }
                // 显示普通消息页（移除管理员判断）
                transview(xiaoXiFragment, VIEW_XIAOXI_INDEX);
                break;
            case VIEW_WODE_INDEX:
                // 必须已登录才能显示"我的"页面
                if (!StaticVar.isLogin(this)) {
                    transview(shouYeFragment, VIEW_SHOUYE_INDEX);
                    currentFragmentIndex = VIEW_SHOUYE_INDEX;
                    rbShouYe.setChecked(true);
                    break;
                }
                transview(woDeFragment, VIEW_WODE_INDEX);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        StaticVar.loadUserInfo(this);

        // 确保当前显示的Fragment与登录状态匹配
        if ((currentFragmentIndex == VIEW_XIAOXI_INDEX || currentFragmentIndex == VIEW_WODE_INDEX)
                && !StaticVar.isLogin(this)) {
            // 如果当前在消息或我的页面，但用户已退出登录，返回首页
            transview(shouYeFragment, VIEW_SHOUYE_INDEX);
            currentFragmentIndex = VIEW_SHOUYE_INDEX;
            rbShouYe.setChecked(true);
        }
    }

    public void switchToNormalMessage() {
        runOnUiThread(() -> {
            // 切换到普通消息页
            transview(xiaoXiFragment, VIEW_XIAOXI_INDEX);
            currentFragmentIndex = VIEW_XIAOXI_INDEX;
            VIEW_LAST_INDEX = VIEW_XIAOXI_INDEX;
            temp_position_index = VIEW_XIAOXI_INDEX;
            // 选中消息按钮
            rbXiaoXi.setChecked(true);
        });
    }
}