package com.example.jianlou.index;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.jianlou.Activity.MainActivity;
import com.example.jianlou.R;
import com.example.jianlou.Util.DateUtil;

public class BuySuccessActivity extends AppCompatActivity {

    private TextView tvOrderNumber;
    private TextView tvPayAmount;
    private TextView tvPayTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_success);

        initViews();
        initData();
        setupClickListeners();
    }

    private void initViews() {
        tvOrderNumber = findViewById(R.id.tv_order_number);
        tvPayAmount = findViewById(R.id.tv_pay_amount);
        tvPayTime = findViewById(R.id.tv_pay_time);
    }

    private void initData() {
        Intent intent = getIntent();
        double amount = intent.getDoubleExtra("order_amount", 0.00);
        String productName = intent.getStringExtra("product_name");
        String orderId = intent.getStringExtra("order_id");

        String orderNumber;
        if (orderId != null && !orderId.isEmpty()) {
            orderNumber = orderId;
        } else {
            orderNumber = generateOrderNumber();
        }

        tvOrderNumber.setText("订单号：" + orderNumber);
        tvPayAmount.setText(String.format("¥ %.2f", amount));
        tvPayTime.setText("支付时间：" + DateUtil.getCurrentDateTime());
    }

    private void setupClickListeners() {

        Button btnContinueShopping = findViewById(R.id.btn_continue_shopping);
        btnContinueShopping.setOnClickListener(v -> {
            Intent intent = new Intent(BuySuccessActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("fragment", "market");
            startActivity(intent);
            finish();
        });
    }

    private String generateOrderNumber() {
        long timestamp = System.currentTimeMillis();
        int random = (int) (Math.random() * 10000);
        return String.format("ORD%tY%tm%td%04d", timestamp, timestamp, timestamp, random);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}