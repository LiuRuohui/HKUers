package hk.hku.cs.hkuers.features.marketplace;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import hk.hku.cs.hkuers.features.trade.TradeListActivity;

public class MarketplaceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 直接跳转到交易列表页面
        Intent intent = new Intent(this, TradeListActivity.class);
        startActivity(intent);
        finish(); // 结束当前Activity
    }
}