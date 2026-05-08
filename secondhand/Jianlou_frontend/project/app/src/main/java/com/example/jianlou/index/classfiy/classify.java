package com.example.jianlou.index.classfiy;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.example.jianlou.R;
import com.facebook.drawee.backends.pipeline.Fresco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类页面
 * 功能：展示左侧分类菜单 + 右侧分类详情，支持左侧点击切换右侧内容、右侧滚动联动左侧选中状态
 */
public class classify extends AppCompatActivity {

    // 左侧分类菜单的标题列表
    private List<String> menuList = new ArrayList<>();
    // 右侧分类详情数据列表（封装了分类标题、图标、文本描述等信息）
    private List<CategoryBean.DataBean> homeList = new ArrayList<>();
    // 记录右侧每个分类板块的起始位置，用于滚动时定位左侧选中项
    private List<Integer> showTitle;

    // 左侧分类菜单ListView
    private ListView lv_menu;
    // 右侧分类详情ListView
    private ListView lv_home;

    // 左侧菜单的适配器
    private MenuAdapter menuAdapter;
    // 右侧详情的适配器
    private HomeAdapter homeAdapter;
    // 当前左侧选中的分类项索引
    private int currentItem;
    // 返回按钮
    private ImageView back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classfiy);
        Fresco.initialize(this);
        // 初始化控件和监听器
        initView();
        // 加载本地JSON分类数据
        loadData();
    }

    /**
     * 加载本地assets目录下的category.json分类数据
     * 解析JSON数据并填充到左侧菜单列表和右侧详情列表
     */
    private void loadData() {
        // 读取assets中的category.json文件内容
        String json = getJson(this, "category.json");
        // 将JSON字符串解析为CategoryBean实体类（FastJson）
        CategoryBean categoryBean = JSONObject.parseObject(json, CategoryBean.class);
        // 初始化右侧分类板块起始位置记录列表
        showTitle = new ArrayList<>();

        // 遍历解析后的分类数据，填充菜单和详情列表
        for (int i = 0; i < categoryBean.getData().size(); i++) {
            CategoryBean.DataBean dataBean = categoryBean.getData().get(i);
            // 左侧菜单添加分类标题
            menuList.add(dataBean.getModuleTitle());
            // 记录右侧该分类板块的起始位置
            showTitle.add(i);
            // 右侧详情添加完整分类数据
            homeList.add(dataBean);
        }
        // 通知左侧菜单适配器刷新数据
        menuAdapter.notifyDataSetChanged();
        // 通知右侧详情适配器刷新数据
        homeAdapter.notifyDataSetChanged();
    }

    /**
     * 初始化页面控件、适配器和各类点击/滚动监听器
     */
    private void initView() {
        // 绑定控件ID
        lv_menu = findViewById(R.id.lv_menu);
        lv_home = findViewById(R.id.lv_home);
        back = findViewById(R.id.classify_back);

        // 返回按钮点击事件：关闭当前分类页面
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 初始化左侧菜单适配器并绑定到ListView
        menuAdapter = new MenuAdapter(this, menuList);
        lv_menu.setAdapter(menuAdapter);
        // 初始化右侧详情适配器并绑定到ListView
        homeAdapter = new HomeAdapter(this, homeList);
        lv_home.setAdapter(homeAdapter);

        // 左侧分类菜单的点击事件：点击左侧项，右侧跳转到对应分类板块
        lv_menu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 设置左侧菜单选中项
                menuAdapter.setSelectItem(position);
                // 通知适配器刷新选中状态
                menuAdapter.notifyDataSetInvalidated();
                // 右侧ListView滚动到对应分类板块的起始位置
                lv_home.setSelection(showTitle.get(position));
            }
        });

        // 右侧分类详情的点击事件：点击右侧分类项，弹出分类名称提示
        lv_home.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 弹出Toast显示当前点击分类的"更多文本"（如分类名称）
                Toast.makeText(classify.this, homeList.get(position).getMoreText(), Toast.LENGTH_SHORT).show();
            }
        });

        // 右侧ListView滚动监听器：滚动时联动左侧菜单选中状态
        lv_home.setOnScrollListener(new AbsListView.OnScrollListener() {
            // 记录滚动状态（空闲/触摸滚动/惯性滚动）
            private int scrollState;

            /**
             * 滚动状态变化时回调
             * @param view 滚动的ListView
             * @param scrollState 滚动状态：SCROLL_STATE_IDLE（空闲）、SCROLL_STATE_TOUCH_SCROLL（触摸滚动）、SCROLL_STATE_FLING（惯性滚动）
             */
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                this.scrollState = scrollState;
            }

            /**
             * 滚动过程中持续回调
             * @param view 滚动的ListView
             * @param firstVisibleItem 第一个可见项的索引
             * @param visibleItemCount 可见项数量
             * @param totalItemCount 总项数
             */
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                // 若滚动状态为空闲，不处理联动
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    return;
                }
                // 获取当前可见项对应的左侧分类索引
                int current = showTitle.indexOf(firstVisibleItem);
                // 若当前索引与选中项不一致，且索引有效，则更新左侧选中状态
                if (currentItem != current && current >= 0) {
                    currentItem = current;
                    // 设置左侧菜单选中项
                    menuAdapter.setSelectItem(currentItem);
                    // 刷新左侧菜单选中状态
                    menuAdapter.notifyDataSetInvalidated();
                }
            }
        });
    }

    /**
     * 读取assets目录下指定名称的JSON文件内容
     * @param context 上下文
     * @param fileName JSON文件名（如"category.json"）
     * @return JSON文件的字符串内容
     */
    public static String getJson(Context context, String fileName) {
        // 拼接JSON文件内容的字符串构建器
        StringBuilder stringBuilder = new StringBuilder();
        // 获取assets资源管理器（用于访问assets目录文件）
        AssetManager assetManager = context.getAssets();
        // 使用IO流读取JSON文件内容
        try {
            // 打开文件并创建缓冲阅读器（指定UTF-8编码避免中文乱码）
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    assetManager.open(fileName), "utf-8"));
            String line;
            // 逐行读取文件内容并拼接
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            // 捕获IO异常并打印堆栈信息
            e.printStackTrace();
        }
        // 返回拼接后的JSON字符串
        return stringBuilder.toString();
    }

    /**
     * 页面停止时回调：关闭当前分类页面（避免页面驻留）
     */
    @Override
    protected void onStop() {
        super.onStop();
        finish();
    }
}