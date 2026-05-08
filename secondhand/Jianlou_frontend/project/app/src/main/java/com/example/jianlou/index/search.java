package com.example.jianlou.index;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jianlou.Internet.HttpUtil;
import com.example.jianlou.R;
import com.example.jianlou.staticVar.StaticVar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

public class search extends AppCompatActivity implements View.OnClickListener, RadioGroup.OnCheckedChangeListener {
    private ImageView back;
    private EditText searchEditText;
    private RadioGroup check;
    private RecyclerView recyclerView;
    private List<Good> goodList = new ArrayList<>();
    private ProgressBar progressBar;
    private int NORMAL_LOADING = 0;
    private int LOADING_MORE = 1;
    private int maxPage = 0;
    private int nowPage = 1;
    private int id = 1;  // 搜索的排序方式：1综合 2时间正序 3时间倒序 4价格升序 5价格降序
    private int state = NORMAL_LOADING;
    private GoodAdapter goodAdapter;
    private boolean isClassifySearch = false;
    private String classifyName = "";

    // 价格筛选相关控件
    private LinearLayout priceFilterLayout;
    private EditText etMinPrice;
    private EditText etMaxPrice;
    private Button btnPriceFilter;
    private Button btnClearFilter;
    private TextView tvPriceFilterInfo;

    // 价格筛选状态
    private boolean priceFilterActive = false;//是否进行价格筛选：默认为false
    private double currentMinPrice = 0;//设置当前默认最低价
    private double currentMaxPrice = Double.MAX_VALUE;//默认值设为 double 类型的最大值

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        init();
    }

    private void init() {
        Intent intent = getIntent();
        String result = intent.getStringExtra("result");//获得index_search中传来的数据
        isClassifySearch = intent.getBooleanExtra("isClassify", false);
        classifyName = intent.getStringExtra("classify");

        // 初始化基础控件
        back = findViewById(R.id.search_back);
        searchEditText = findViewById(R.id.search_search);
        check = findViewById(R.id.search_check);
        recyclerView = findViewById(R.id.search_recycle_view);
        progressBar = findViewById(R.id.search_progress);

        // 初始化价格筛选相关控件
        priceFilterLayout = findViewById(R.id.price_filter_layout);
        etMinPrice = findViewById(R.id.et_min_price);
        etMaxPrice = findViewById(R.id.et_max_price);
        btnPriceFilter = findViewById(R.id.btn_price_filter);
        btnClearFilter = findViewById(R.id.btn_clear_filter);
        tvPriceFilterInfo = findViewById(R.id.tv_price_filter_info);//显示价格范围

        // 设置点击监听
        back.setOnClickListener(this);
        searchEditText.setOnClickListener(this);
        btnPriceFilter.setOnClickListener(this);
        btnClearFilter.setOnClickListener(this);

        // 设置搜索框文本
        searchEditText.setText(result);

        // 如果是分类搜索，禁用搜索框
        if (isClassifySearch) {
            searchEditText.setEnabled(false);
            searchEditText.setFocusable(false);
            searchEditText.setFocusableInTouchMode(false);
            searchEditText.setTextColor(Color.GRAY);
            // 设置标题显示分类名称
            if (classifyName != null) {
                setTitle(classifyName + " - 分类商品");
            }
        }

        // 价格筛选区域对所有搜索类型都显示
        priceFilterLayout.setVisibility(View.VISIBLE);

        // 设置排序选项监听
        check.setOnCheckedChangeListener(this);

        // 设置RecyclerView
        StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(layoutManager);
        goodAdapter = new GoodAdapter(goodList);
        recyclerView.setAdapter(goodAdapter);

        // 加载商品数据
        initGood();

        // 设置滚动加载监听
        recyclerView.addOnScrollListener(new EndlessRecyclerOnScrollListener() {
            @Override
            public void onLoadMore() {
                goodAdapter.setLoadState(GoodAdapter.LOADING);
                if (nowPage <= maxPage) {
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    state = LOADING_MORE;
                                    initGood();
                                    goodAdapter.setLoadState(GoodAdapter.LOADING_COMPLETE);
                                }
                            });
                        }
                    }, 0);
                } else {
                    goodAdapter.setLoadState(GoodAdapter.LOADING_END);
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.search_back:
                finish();
                break;
            case R.id.search_search:
                    Intent intent = new Intent(search.this, index_search.class);
                    startActivity(intent);
                break;
            case R.id.btn_price_filter:
                applyPriceFilter();//应用筛选
                break;
            case R.id.btn_clear_filter:
                clearPriceFilter();//清除筛选
                break;
        }
    }

    // 应用价格筛选
    private void applyPriceFilter() {
        try {
            String minStr = etMinPrice.getText().toString().trim();
            String maxStr = etMaxPrice.getText().toString().trim();

            // 如果两个都为空，不进行筛选
            if (minStr.isEmpty() && maxStr.isEmpty()) {
                priceFilterActive = false;
                tvPriceFilterInfo.setText("");
                reloadGoods();
                return;
            }

            // 解析最低价
            if (!minStr.isEmpty()) {
                currentMinPrice = Double.parseDouble(minStr);
                if (currentMinPrice < 0) {
                    Toast.makeText(this, "价格不能为负数", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                currentMinPrice = 0;
            }

            // 解析最高价
            if (!maxStr.isEmpty()) {
                currentMaxPrice = Double.parseDouble(maxStr);
                if (currentMaxPrice < 0) {
                    Toast.makeText(this, "价格不能为负数", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (currentMaxPrice < currentMinPrice) {
                    Toast.makeText(this, "最高价不能低于最低价", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                currentMaxPrice = Double.MAX_VALUE;
            }

            priceFilterActive = true;

            // 更新筛选信息显示
            updateFilterInfo();
            // 重新加载商品
            reloadGoods();

            Toast.makeText(this, "价格筛选已应用", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的价格数字", Toast.LENGTH_SHORT).show();
        }
    }

    // 清除价格筛选
    private void clearPriceFilter() {
        etMinPrice.setText("");
        etMaxPrice.setText("");
        currentMinPrice = 0;
        currentMaxPrice = Double.MAX_VALUE;
        priceFilterActive = false;
        tvPriceFilterInfo.setText("");

        // 重新加载商品
        reloadGoods();

        Toast.makeText(this, "价格筛选已清除", Toast.LENGTH_SHORT).show();
    }

    // 更新筛选信息显示
    private void updateFilterInfo() {
        StringBuilder info = new StringBuilder("当前筛选：");

        if (currentMinPrice > 0) {
            info.append(currentMinPrice).append("元");
        } else {
            info.append("不限");
        }

        info.append(" ~ ");

        if (currentMaxPrice < Double.MAX_VALUE) {
            info.append(currentMaxPrice).append("元");
        } else {
            info.append("不限");
        }

        tvPriceFilterInfo.setText(info.toString());
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // 两种搜索都处理排序方式
        switch (checkedId) {
            case R.id.search_check1:
                id = 1;  // 综合排序
                break;
            case R.id.search_check2:
                id = 2;  // 时间正序
                break;
            case R.id.search_check3:
                id = 3;  // 时间倒序
                break;
            case R.id.search_check4:
                id = 4;  // 价格升序
                break;
            case R.id.search_check5:
                id = 5;  // 价格降序
                break;
        }
        // 重新加载商品
        reloadGoods();
    }

    // 重新加载商品
    private void reloadGoods() {
        goodAdapter.setLoadState(GoodAdapter.LOADING_COMPLETE);
        state = NORMAL_LOADING;
        nowPage = 1;
        maxPage = 0;
        initGood();//
    }

    //构建请求体
    private void initGood() {
        progressBar.setVisibility(View.VISIBLE);
        if (state == NORMAL_LOADING) {
            goodList.clear();
        }

        RequestBody requestBody;
        String url;

        if (isClassifySearch) {
            // 分类搜索 - 构建请求参数
            FormBody.Builder builder = new FormBody.Builder()
                    .add("classify", classifyName)
                    .add("page", String.valueOf(nowPage))
                    .add("sortType", String.valueOf(id));  // 使用id作为排序类型

            // 如果有价格筛选，添加价格参数
            if (priceFilterActive) {
                builder.add("minPrice", String.valueOf(currentMinPrice))
                        .add("maxPrice", String.valueOf(currentMaxPrice));
            }

            requestBody = builder.build();
            url = StaticVar.BaseUrl + "/search/byClassify/";

        } else {
            // 普通关键词搜索 - 构建请求参数
            FormBody.Builder builder = new FormBody.Builder()
                    .add("method", String.valueOf(id))  // 排序方式
                    .add("q", searchEditText.getText().toString())  // 搜索关键词
                    .add("page", String.valueOf(nowPage));  // 页码

            // 如果有价格筛选，添加价格参数
            if (priceFilterActive) {
                builder.add("minPrice", String.valueOf(currentMinPrice))
                        .add("maxPrice", String.valueOf(currentMaxPrice));
            }

            requestBody = builder.build();
            url = StaticVar.searchUrl;
        }

        HttpUtil.sendOkHttpRequest(url, requestBody, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateUI();
                outputMessage("请求失败，请检查网络");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response(response);
                updateUI();
            }
        });
    }

    private void response(Response response) throws IOException {
        if (!response.isSuccessful()) {
            outputMessage("服务器故障: " + response.code());
            return;
        }

        String responseData = response.body() != null ? response.body().string() : "";

        // 1. 处理特殊字符串响应
        if (responseData.isEmpty() || responseData.equals("[]") || responseData.equals("failed")) {
            handleEmptyOrFailed(responseData);
            return;
        }

        try {
            JSONArray jsonArray = new JSONArray(responseData);
            int totalItems = jsonArray.length();
            if (totalItems == 0) {
                handleEmptyOrFailed("[]");
                return;
            }

            // 2. 识别分页信息（通常在数组最后一位）
            JSONObject lastObject = jsonArray.optJSONObject(totalItems - 1);
            boolean hasPageInfo = lastObject != null && (lastObject.has("max_page") || lastObject.has("total_items"));
            int goodsCount = hasPageInfo ? totalItems - 1 : totalItems;

            // 3. 循环解析商品 (只打印解析失败的异常)
            for (int i = 0; i < goodsCount; i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String money = jsonObject.optString("money");
                String content = jsonObject.optString("content");
                String goodID = jsonObject.optString("goodsID");
                String user_name = jsonObject.optString("user_name");

                if (money.isEmpty() || content.isEmpty() || goodID.isEmpty()) continue;

                String imageUrl = formatImageUrl(jsonObject.optString("image", ""));
                goodList.add(new Good(Uri.parse(imageUrl), R.mipmap.cat, content, money, user_name, goodID));
            }

            // 4. 更新UI
            final int finalMaxPage = hasPageInfo ? lastObject.optInt("max_page", 0) : maxPage;
            runOnUiThread(() -> {
                if (hasPageInfo) {
                    nowPage++;
                    maxPage = finalMaxPage;
                }
                goodAdapter.notifyDataSetChanged();
            });

        } catch (JSONException e) {
            Log.e("ResponseError", "JSON parse error", e);
            outputMessage("数据解析错误");
        }
    }

    // 提取出的辅助方法，让主逻辑更清晰
    private String formatImageUrl(String url) {
        if (url.isEmpty() || url.startsWith("http")) return url;
        return url.startsWith("/upload/") ? StaticVar.BaseUrl + url : StaticVar.BaseUrl + "/" + url;
    }

    private void handleEmptyOrFailed(String status) {
        runOnUiThread(() -> {
            if ("failed".equals(status)) {
                outputMessage("服务器错误");
            } else {
                String msg = isClassifySearch ? "该分类下暂无商品" : "未找到相关商品";
                Toast.makeText(search.this, msg, Toast.LENGTH_SHORT).show();
                maxPage = 0;
                goodAdapter.notifyDataSetChanged();
            }
        });
    }

    private void outputMessage(String message) {
        runOnUiThread(() -> Toast.makeText(search.this, message, Toast.LENGTH_SHORT).show());
    }

    private void updateUI() {
        runOnUiThread(() -> progressBar.setVisibility(View.GONE));
    }
}